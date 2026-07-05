#!/usr/bin/env node
import { mkdir, writeFile } from "node:fs/promises";
import { performance } from "node:perf_hooks";
import path from "node:path";

const BASE_URL = normalizeBaseUrl(process.env.BASE_URL || "http://127.0.0.1:9001");
const EMPLOYEE_ID = process.env.EMPLOYEE_ID || "SUPER_ADMIN_001";
const PASSWORD = process.env.PASSWORD || "admin123";
const AUTH_TOKEN = process.env.AUTH_TOKEN || "";
const ITERATIONS = Number(process.env.ITERATIONS || 25);
const WARMUP = Number(process.env.WARMUP || 3);
const CONCURRENCY = Number(process.env.CONCURRENCY || 1);
const RESULTS_DIR = process.env.RESULTS_DIR || "tools/performance/results";
const SAMPLE_MOBILE = process.env.SAMPLE_MOBILE || "9000000001";
const SAMPLE_SEARCH = process.env.SAMPLE_SEARCH || SAMPLE_MOBILE;
const SAMPLE_TICKET_ID = process.env.SAMPLE_TICKET_ID || "";

const endpoints = [
  {
    name: "ticket-list-page-0",
    method: "GET",
    path: "/volt/tickets?page=0&size=20",
    ok: [200],
  },
  {
    name: "my-tickets-page-0",
    method: "GET",
    path: "/volt/tickets/my?page=0&size=20",
    ok: [200],
  },
  {
    name: "ticket-search-mobile",
    method: "GET",
    path: `/volt/tickets/search?query=${encodeURIComponent(SAMPLE_SEARCH)}&page=0&size=20`,
    ok: [200],
  },
  {
    name: "ticket-query-date-status",
    method: "GET",
    path: "/volt/tickets/query?status=NEW&page=0&size=20",
    ok: [200],
  },
  {
    name: "customer-lookup-mobile",
    method: "GET",
    path: `/volt/tickets/customer-lookup?mobileNumber=${encodeURIComponent(SAMPLE_MOBILE)}`,
    ok: [200, 204],
  },
];

if (SAMPLE_TICKET_ID) {
  endpoints.push(
    {
      name: "ticket-detail",
      method: "GET",
      path: `/volt/tickets/${encodeURIComponent(SAMPLE_TICKET_ID)}`,
      ok: [200],
    },
    {
      name: "customer-history",
      method: "GET",
      path: `/volt/tickets/${encodeURIComponent(SAMPLE_TICKET_ID)}/customer-history`,
      ok: [200, 403],
    }
  );
}

const createTicketPayload = {
  customerName: "Perf API Create",
  mobileNumber: "9888800001",
  villageOrArea: "Perf Area API",
  productType: "Perf Product",
  categoryId: null,
  complaintDescription: "Performance test create ticket request",
  dynamicValues: [],
};

function normalizeBaseUrl(value) {
  return value.replace(/\/+$/, "");
}

async function login() {
  if (AUTH_TOKEN) return AUTH_TOKEN;

  const response = await fetch(`${BASE_URL}/volt/auth/employeelogin`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ employeeId: EMPLOYEE_ID, password: PASSWORD }),
  });

  if (!response.ok) {
    const body = await response.text();
    throw new Error(`Login failed: ${response.status} ${body}`);
  }

  const data = await response.json();
  if (!data.token) throw new Error("Login response did not include token");
  return data.token;
}

async function findCategoryId(token) {
  const response = await fetch(`${BASE_URL}/volt/ticket-categories`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!response.ok) return null;
  const data = await response.json();
  const categories = Array.isArray(data) ? data : Array.isArray(data?.categories) ? data.categories : [];
  return categories.find((category) => category.active)?.id ?? null;
}

async function timeRequest(token, endpoint) {
  const startedAt = performance.now();
  const response = await fetch(`${BASE_URL}${endpoint.path}`, {
    method: endpoint.method,
    headers: {
      Authorization: `Bearer ${token}`,
      ...(endpoint.body ? { "Content-Type": "application/json" } : {}),
    },
    body: endpoint.body ? JSON.stringify(endpoint.body) : undefined,
  });
  const bodyText = await response.text();
  const durationMs = performance.now() - startedAt;
  const bytes = Buffer.byteLength(bodyText);

  if (!endpoint.ok.includes(response.status)) {
    throw new Error(`${endpoint.name} returned ${response.status}: ${bodyText.slice(0, 300)}`);
  }

  return {
    durationMs,
    status: response.status,
    bytes,
  };
}

async function runEndpoint(token, endpoint) {
  for (let i = 0; i < WARMUP; i += 1) {
    await timeRequest(token, endpoint);
  }

  const samples = [];
  let next = 0;

  async function worker() {
    while (next < ITERATIONS) {
      next += 1;
      samples.push(await timeRequest(token, endpoint));
    }
  }

  await Promise.all(Array.from({ length: CONCURRENCY }, () => worker()));
  return summarize(endpoint.name, samples);
}

function percentile(sortedValues, p) {
  if (sortedValues.length === 0) return 0;
  const index = Math.ceil((p / 100) * sortedValues.length) - 1;
  return sortedValues[Math.max(0, Math.min(index, sortedValues.length - 1))];
}

function summarize(name, samples) {
  const durations = samples.map((sample) => sample.durationMs).sort((a, b) => a - b);
  const bytes = samples.map((sample) => sample.bytes);
  return {
    name,
    iterations: samples.length,
    statusCodes: [...new Set(samples.map((sample) => sample.status))],
    minMs: round(durations[0]),
    p50Ms: round(percentile(durations, 50)),
    p95Ms: round(percentile(durations, 95)),
    p99Ms: round(percentile(durations, 99)),
    maxMs: round(durations[durations.length - 1]),
    avgBytes: Math.round(bytes.reduce((sum, value) => sum + value, 0) / bytes.length),
  };
}

function round(value) {
  return Math.round(value * 10) / 10;
}

async function main() {
  console.log(`Base URL: ${BASE_URL}`);
  console.log(`Iterations: ${ITERATIONS}, warmup: ${WARMUP}, concurrency: ${CONCURRENCY}`);

  const token = await login();
  const categoryId = await findCategoryId(token);
  if (categoryId) {
    endpoints.push({
      name: "create-ticket",
      method: "POST",
      path: "/volt/tickets",
      body: { ...createTicketPayload, categoryId },
      ok: [201],
    });
  } else {
    console.warn("No active category found; skipping create-ticket benchmark.");
  }

  const results = [];
  for (const endpoint of endpoints) {
    process.stdout.write(`Running ${endpoint.name}... `);
    const result = await runEndpoint(token, endpoint);
    results.push(result);
    console.log(`p95=${result.p95Ms}ms p99=${result.p99Ms}ms bytes=${result.avgBytes}`);
  }

  await mkdir(RESULTS_DIR, { recursive: true });
  const report = {
    generatedAt: new Date().toISOString(),
    baseUrl: BASE_URL,
    employeeId: EMPLOYEE_ID,
    iterations: ITERATIONS,
    warmup: WARMUP,
    concurrency: CONCURRENCY,
    sampleMobile: SAMPLE_MOBILE,
    sampleSearch: SAMPLE_SEARCH,
    sampleTicketId: SAMPLE_TICKET_ID || null,
    results,
  };
  const outputPath = path.join(RESULTS_DIR, `api-performance-${new Date().toISOString().replace(/[:.]/g, "-")}.json`);
  await writeFile(outputPath, `${JSON.stringify(report, null, 2)}\n`);
  console.log(`Wrote ${outputPath}`);
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
