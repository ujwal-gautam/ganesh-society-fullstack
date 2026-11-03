import React, { useEffect, useState } from "react";
import { createRoot } from "react-dom/client";
import "./style.css";
const API = import.meta.env.VITE_API_URL || "http://localhost:8080";
const rupee = (n) =>
  "₹" + Number(n || 0).toLocaleString("en-IN", { maximumFractionDigits: 2 });
const today = () => new Date().toISOString().slice(0, 10);
async function request(path, method = "GET", body, credentials) {
  const headers = { "Content-Type": "application/json" };
  if (credentials)
    headers.Authorization =
      "Basic " + btoa(credentials.email + ":" + credentials.password);
  const r = await fetch(API + path, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  });
  if (!r.ok)
    throw Error(
      r.status === 401
        ? "Invalid login or session"
        : `Request failed (${r.status})`,
    );
  return r.json();
}
function App() {
  const [data, setData] = useState(null),
    [admin, setAdmin] = useState(false),
    [auth, setAuth] = useState(null),
    [login, setLogin] = useState({ email: "", password: "" }),
    [error, setError] = useState(""),
    [tab, setTab] = useState("overview");
  const refresh = async () => {
    try {
      setData(await request("/api/public/snapshot"));
    } catch (e) {
      setError(e.message);
    }
  };
  useEffect(() => {
    refresh();
    const timer = setInterval(refresh, 15000);
    return () => clearInterval(timer);
  }, []);
  async function signIn(e) {
    e.preventDefault();
    try {
      await request("/api/admin/me", "GET", null, login);
      setAuth({ ...login });
      setLogin({ email: "", password: "" });
      setAdmin(true);
      setError("");
    } catch (e) {
      setError(e.message);
    }
  }
  async function save(path, body, method = "POST") {
    try {
      await request("/api/admin" + path, method, body, auth);
      await refresh();
      setError("");
      alert("Saved successfully");
    } catch (e) {
      setError(e.message);
    }
  }
  if (!data)
    return (
      <main>
        <h1>Shree Ganesh Society</h1>
        <p>Loading… {error}</p>
      </main>
    );
  const { members, contributions, loans, repayments, settings } = data;
  const memberName = (id) =>
    members.find((m) => m.id === id)?.fullName || "Unknown";
  const totalContrib = contributions.reduce((s, c) => s + Number(c.amount), 0);
  const principalPaid = (id) =>
    repayments
      .filter((r) => r.loanId === id)
      .reduce((s, r) => s + Number(r.principalComponent), 0);
  const outstanding = loans.reduce(
    (s, l) => s + Math.max(0, Number(l.principal) - principalPaid(l.id)),
    0,
  );
  const interestCollected = repayments.reduce(
    (s, r) => s + Number(r.interestComponent),
    0,
  );
  const cash =
    totalContrib +
    repayments.reduce((s, r) => s + Number(r.receivedAmount), 0) -
    loans.reduce((s, l) => s + Number(l.principal), 0);
  const memberCount = members.filter((m) => m.active).length;
  return (
    <>
      <header>
        <div className="shell">
          <div className="eyebrow">Savings Group Passbook</div>
          <h1>{settings.groupName}</h1>
          <p>
            {memberCount} active members · {settings.monthlyInterestRate}%
            monthly loan interest
          </p>
          <div className="headline">
            <div>
              <small>Available cash (recorded)</small>
              <strong>{rupee(cash)}</strong>
            </div>
            <div>
              <small>Outstanding principal</small>
              <strong>{rupee(outstanding)}</strong>
            </div>
          </div>
        </div>
      </header>
      <main className="shell">
        <nav>
          {[
            "overview",
            "members",
            "contributions",
            "loans",
            ...(admin ? ["admin"] : []),
          ].map((t) => (
            <button
              key={t}
              className={tab === t ? "selected" : ""}
              onClick={() => setTab(t)}
            >
              {t}
            </button>
          ))}
          <button
            className="secondary"
            onClick={() => {
              setAdmin(!admin);
              setTab(admin ? "overview" : "admin");
            }}
          >
            {admin ? "Close admin" : "Admin"}
          </button>
        </nav>
        {error && <p className="error">{error}</p>}
        {tab === "overview" && (
          <>
            <section className="stats">
              <article>
                <small>Contributions received</small>
                <b>{rupee(totalContrib)}</b>
              </article>
              <article>
                <small>Loan principal outstanding</small>
                <b>{rupee(outstanding)}</b>
              </article>
              <article>
                <small>Interest allocated</small>
                <b>{rupee(interestCollected)}</b>
              </article>
              <article>
                <small>Total shares</small>
                <b>
                  {members
                    .filter((m) => m.active)
                    .reduce((s, m) => s + m.shares, 0)}
                </b>
              </article>
            </section>
            <section className="panel">
              <h2>Monthly collection</h2>
              <table>
                <thead>
                  <tr>
                    <th>Month</th>
                    <th>Collected</th>
                  </tr>
                </thead>
                <tbody>
                  {[...new Set(contributions.map((c) => c.contributionMonth))]
                    .sort()
                    .map((m) => (
                      <tr key={m}>
                        <td>{m}</td>
                        <td>
                          {rupee(
                            contributions
                              .filter((c) => c.contributionMonth === m)
                              .reduce((s, c) => s + Number(c.amount), 0),
                          )}
                        </td>
                      </tr>
                    ))}
                </tbody>
              </table>
            </section>
          </>
        )}
        {tab === "members" && (
          <section className="panel">
            <h2>Members</h2>
            <table>
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Shares</th>
                  <th>Monthly due</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {members.map((m) => (
                  <tr key={m.id}>
                    <td>{m.fullName}</td>
                    <td>{m.shares}</td>
                    <td>{rupee(m.shares * settings.monthlyAmountPerShare)}</td>
                    <td>{m.active ? "Active" : "Inactive"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </section>
        )}
        {tab === "contributions" && (
          <section className="panel">
            <h2>Contribution ledger</h2>
            <table>
              <thead>
                <tr>
                  <th>Member</th>
                  <th>Month</th>
                  <th>Amount</th>
                  <th>Date</th>
                </tr>
              </thead>
              <tbody>
                {[...contributions]
                  .sort((a, b) => b.paidOn.localeCompare(a.paidOn))
                  .map((c) => (
                    <tr key={c.id}>
                      <td>{memberName(c.memberId)}</td>
                      <td>{c.contributionMonth}</td>
                      <td>{rupee(c.amount)}</td>
                      <td>{c.paidOn}</td>
                    </tr>
                  ))}
              </tbody>
            </table>
          </section>
        )}
        {tab === "loans" && (
          <section className="panel">
            <h2>Loan ledger</h2>
            <table>
              <thead>
                <tr>
                  <th>Member</th>
                  <th>Issued</th>
                  <th>Principal</th>
                  <th>Balance</th>
                  <th>Notes</th>
                </tr>
              </thead>
              <tbody>
                {loans.map((l) => (
                  <tr key={l.id}>
                    <td>{memberName(l.memberId)}</td>
                    <td>{l.issuedOn}</td>
                    <td>{rupee(l.principal)}</td>
                    <td>{rupee(Number(l.principal) - principalPaid(l.id))}</td>
                    <td>
                      {l.needsReview ? "⚠ Needs review" : ""} {l.notes}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            <h3>Repayments</h3>
            <table>
              <thead>
                <tr>
                  <th>Member</th>
                  <th>Date</th>
                  <th>Received</th>
                  <th>Principal</th>
                  <th>Interest</th>
                </tr>
              </thead>
              <tbody>
                {repayments.map((r) => (
                  <tr key={r.id}>
                    <td>
                      {memberName(
                        loans.find((l) => l.id === r.loanId)?.memberId,
                      )}
                    </td>
                    <td>{r.paidOn}</td>
                    <td>{rupee(r.receivedAmount)}</td>
                    <td>{rupee(r.principalComponent)}</td>
                    <td>{rupee(r.interestComponent)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </section>
        )}
        {tab === "admin" &&
          (auth ? (
            <>
              <div className="adminbar">
                Logged in as {auth.email}{" "}
                <button
                  onClick={() => {
                    setAuth(null);
                    setTab("overview");
                    setAdmin(false);
                  }}
                >
                  Logout
                </button>
              </div>
              <AdminForms
                members={members}
                loans={loans}
                settings={settings}
                save={save}
              />
            </>
          ) : (
            <section className="panel narrow">
              <h2>Admin login</h2>
              <p>Only authorized administrators can change records.</p>
              <form onSubmit={signIn}>
                <label>
                  Email
                  <input
                    type="email"
                    required
                    value={login.email}
                    onChange={(e) =>
                      setLogin({ ...login, email: e.target.value })
                    }
                  />
                </label>
                <label>
                  Password
                  <input
                    type="password"
                    required
                    value={login.password}
                    onChange={(e) =>
                      setLogin({ ...login, password: e.target.value })
                    }
                  />
                </label>
                <button>Sign in</button>
              </form>
            </section>
          ))}
      </main>
      <footer>
        Shree Ganesh Society · Read-only public passbook · Auto-refresh every 15
        seconds
      </footer>
    </>
  );
}
function AdminForms({ members, loans, settings, save }) {
  const [member, setMember] = useState({ fullName: "", shares: 1 }),
    [contribution, setContribution] = useState({
      memberId: "",
      contributionMonth: today().slice(0, 7),
      amount: "",
      paidOn: today(),
    }),
    [loan, setLoan] = useState({
      memberId: "",
      principal: "",
      termMonths: 3,
      issuedOn: today(),
      needsReview: false,
    }),
    [repayment, setRepayment] = useState({
      loanId: "",
      receivedAmount: "",
      principalComponent: "",
      interestComponent: "",
      paidOn: today(),
    }),
    [config, setConfig] = useState(settings);
  const form = (title, fields, state, update, submit) => (
    <section className="panel">
      <h2>{title}</h2>
      <form
        onSubmit={(e) => {
          e.preventDefault();
          submit();
        }}
      >
        <div className="fields">
          {fields.map(([key, label, type, options]) => (
            <label key={key}>
              {label}
              {type === "select" ? (
                <select
                  required
                  value={state[key]}
                  onChange={(e) =>
                    update({ ...state, [key]: Number(e.target.value) })
                  }
                >
                  <option value="">Select</option>
                  {options.map((o) => (
                    <option key={o.id} value={o.id}>
                      {o.fullName || o.label}
                    </option>
                  ))}
                </select>
              ) : (
                <input
                  required={key !== "notes"}
                  type={type}
                  step={type === "number" ? "any" : undefined}
                  value={state[key] ?? ""}
                  onChange={(e) => update({ ...state, [key]: e.target.value })}
                />
              )}
            </label>
          ))}
        </div>
        <button type="submit">Save {title}</button>
      </form>
    </section>
  );
  return (
    <div className="admin-grid">
      {form(
        "New member",
        [
          ["fullName", "Full name", "text"],
          ["shares", "Shares", "number"],
        ],
        member,
        setMember,
        () => save("/members", { ...member, shares: Number(member.shares) }),
      )}
      {form(
        "Record contribution",
        [
          ["memberId", "Member", "select", members.filter((m) => m.active)],
          ["contributionMonth", "Month", "month"],
          ["amount", "Amount ₹", "number"],
          ["paidOn", "Payment date", "date"],
        ],
        contribution,
        setContribution,
        () =>
          save("/contributions", {
            ...contribution,
            memberId: Number(contribution.memberId),
            amount: Number(contribution.amount),
          }),
      )}
      {form(
        "Issue loan",
        [
          ["memberId", "Member", "select", members.filter((m) => m.active)],
          ["principal", "Principal ₹", "number"],
          ["termMonths", "Term (months)", "number"],
          ["issuedOn", "Issue date", "date"],
        ],
        loan,
        setLoan,
        () =>
          save("/loans", {
            ...loan,
            memberId: Number(loan.memberId),
            principal: Number(loan.principal),
            termMonths: Number(loan.termMonths),
          }),
      )}
      {form(
        "Record loan repayment",
        [
          [
            "loanId",
            "Loan",
            "select",
            loans.map((l) => ({
              id: l.id,
              label: `Loan #${l.id} — ₹${l.principal}`,
            })),
          ],
          ["receivedAmount", "Received ₹", "number"],
          ["principalComponent", "Principal allocation ₹", "number"],
          ["interestComponent", "Interest allocation ₹", "number"],
          ["paidOn", "Date", "date"],
        ],
        repayment,
        setRepayment,
        () =>
          save("/loans/" + repayment.loanId + "/repayments", {
            receivedAmount: Number(repayment.receivedAmount),
            principalComponent: Number(repayment.principalComponent),
            interestComponent: Number(repayment.interestComponent),
            paidOn: repayment.paidOn,
          }),
      )}
      {form(
        "Society settings",
        [
          ["groupName", "Group name", "text"],
          ["startMonth", "Cycle starts", "month"],
          ["cycleMonths", "Cycle months", "number"],
          ["monthlyAmountPerShare", "Monthly amount per share ₹", "number"],
          ["monthlyInterestRate", "Monthly interest %", "number"],
        ],
        config,
        setConfig,
        () =>
          save(
            "/settings",
            {
              ...config,
              cycleMonths: Number(config.cycleMonths),
              monthlyAmountPerShare: Number(config.monthlyAmountPerShare),
              monthlyInterestRate: Number(config.monthlyInterestRate),
            },
            "PUT",
          ),
      )}
    </div>
  );
}
createRoot(document.getElementById("root")).render(<App />);
