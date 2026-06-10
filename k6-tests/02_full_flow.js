// k6-tests/02_full_flow.js

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';

const successfulFlows = new Counter('successful_complete_flows');
const failedFlows     = new Counter('failed_flows');

export const options = {
    vus: 5,
    iterations: 5,
    thresholds: {
        successful_complete_flows: ['count>=3'],
        http_req_failed: ['rate<0.05'],
    },
};

const GATEWAY  = 'http://localhost:8090';
const IDENTITY = 'http://localhost:8081';

const TEST_PHONES = [
    '+212600000001',
    '+212600000002',
    '+212600000003',
    '+212600000004',
    '+212600000005',
];

// Derive a readable user label from phone number, e.g. "+212600000003" → "user-3"
function userName(phone) {
    const digits = phone.replace(/\D/g, '');
    return `user-${parseInt(digits.slice(-1), 10)}`;
}

// ─── handleSetup / teardown are not used here.
// Instead we use a shared summary via the k6 handleSummary hook. ───

export default function () {
    const phone  = TEST_PHONES[(__VU - 1) % TEST_PHONES.length];
    const user   = userName(phone);
    const gameId = 1;

    // ────────────────────────────────────────────────────────────
    // ÉTAPE 1 — Demander l'OTP
    // ────────────────────────────────────────────────────────────
    const otpReq = http.post(
        `${GATEWAY}/v1/identity/verify`,
        JSON.stringify({ phone }),
        { headers: { 'Content-Type': 'application/json' } }
    );

    if (!check(otpReq, { 'OTP request 200': (r) => r.status === 200 })) {
        console.error(`[${user}] OTP request FAILED (HTTP ${otpReq.status})`);
        failedFlows.add(1);
        return;
    }

    sleep(1);
    const otpFetchRes = http.get(
        `${IDENTITY}/v1/identity/dev/otp?phone=${encodeURIComponent(phone)}`
    );

    if (!check(otpFetchRes, { 'OTP fetch 200': (r) => r.status === 200 })) {
        console.error(`[${user}] Cannot fetch OTP from dev endpoint`);
        failedFlows.add(1);
        return;
    }
    const otp = JSON.parse(otpFetchRes.body).otp;

    // ────────────────────────────────────────────────────────────
    // ÉTAPE 2 — Valider l'OTP → obtenir le JWT
    // ────────────────────────────────────────────────────────────
    const confirmRes = http.post(
        `${GATEWAY}/v1/identity/confirm-otp`,
        JSON.stringify({ phone, otp }),
        { headers: { 'Content-Type': 'application/json' } }
    );

    if (!check(confirmRes, { 'JWT received 200': (r) => r.status === 200 })) {
        console.error(`[${user}] OTP confirmation FAILED: ${confirmRes.body}`);
        failedFlows.add(1);
        return;
    }
    const jwt = JSON.parse(confirmRes.body).accessToken;
    const authHeader = { 'Authorization': `Bearer ${jwt}` };

    // ────────────────────────────────────────────────────────────
    // ÉTAPE 3 — Rejoindre la queue
    // ────────────────────────────────────────────────────────────
    const joinRes = http.post(
        `${GATEWAY}/v1/queue/join`,
        JSON.stringify({ gameId }),
        { headers: { ...authHeader, 'Content-Type': 'application/json' } }
    );

    check(joinRes, {
        'queue join accepted': (r) => r.status === 200 || r.status === 202,
    });

    const joinBody = JSON.parse(joinRes.body);
    const joinPos  = joinBody.position ?? joinBody.rank ?? '?';
    console.log(`User ${user} joined at position ${joinPos}`);

    // ────────────────────────────────────────────────────────────
    // ÉTAPE 4 — Polling du statut jusqu'à admission (max 60s)
    // ────────────────────────────────────────────────────────────
    let admitted       = false;
    let admissionToken = null;
    let lastPos        = joinPos;
    let attempts       = 0;

    while (!admitted && attempts < 6) {   // 6 × 10s = 60s max
        sleep(10);
        attempts++;

        const pollRes = http.get(
            `${GATEWAY}/v1/queue/status?gameId=${gameId}`,
            { headers: authHeader }
        );

        if (pollRes.status !== 200) {
            console.warn(`[${user}] Poll ${attempts} failed (HTTP ${pollRes.status})`);
            continue;
        }

        const pollBody = JSON.parse(pollRes.body);
        lastPos = pollBody.position ?? pollBody.rank ?? lastPos;

        if (pollBody.status === 'RELEASED') {
            admitted       = true;
            admissionToken = pollBody.admissionToken;
        }
    }

    check({ admitted }, {
        'User admitted within 60s': (v) => v.admitted === true,
    });

    if (admitted) {
        console.log(`✅ User ${user} ADMITTED — token: ${admissionToken}`);
        successfulFlows.add(1);
    } else {
        console.log(`⏳ User ${user} still WAITING at position ${lastPos}`);
        failedFlows.add(1);
    }
}

// ────────────────────────────────────────────────────────────────
// RAPPORT FINAL — affiché après la fin de tous les VUs
// ────────────────────────────────────────────────────────────────
export function handleSummary(data) {
    const total    = data.metrics['successful_complete_flows']
        ? (data.metrics['successful_complete_flows'].values.count || 0)
        + (data.metrics['failed_flows']?.values.count || 0)
        : 5;
    const admitted = data.metrics['successful_complete_flows']?.values.count || 0;
    const waiting  = data.metrics['failed_flows']?.values.count || 0;
    const rate     = total > 0 ? ((admitted / total) * 100).toFixed(1) : '0.0';

    const sep = '='.repeat(43);
    const report = [
        '',
        sep,
        '     FAIRSEAT QUEUE REPORT',
        sep,
        `  Total users tested  : ${total}`,
        `  Admitted (RELEASED) : ${admitted}`,
        `  Still waiting       : ${waiting}`,
        `  Admission rate      : ${rate}%`,
        sep,
        '',
    ].join('\n');

    // Print to stdout and return empty string so k6 doesn't also
    // write its default JSON summary (cleaner terminal output).
    console.log(report);
    return { stdout: report };
}