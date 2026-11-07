import React, { useEffect, useState } from "react";
import { createRoot } from "react-dom/client";
import "./style.css";
const API = import.meta.env.VITE_API_URL || "http://localhost:8080";
const rupee = (n) =>
  "₹" + Number(n || 0).toLocaleString("en-IN", { maximumFractionDigits: 2 });
const today = () => {
  const parts = new Intl.DateTimeFormat("en-US", {timeZone:"Asia/Kolkata",year:"numeric",month:"2-digit",day:"2-digit"}).formatToParts(new Date());
  const get = (key) => parts.find(p => p.type === key).value;
  return `${get("year")}-${get("month")}-${get("day")}`;
};
const monthOffset = (offset) => {
  const parts = new Intl.DateTimeFormat("en-US", {timeZone:"Asia/Kolkata",year:"numeric",month:"numeric"}).formatToParts(new Date());
  const year = Number(parts.find(p=>p.type==="year").value);
  const month = Number(parts.find(p=>p.type==="month").value);
  const date = new Date(Date.UTC(year, month - 1 + offset, 1));
  return `${date.getUTCFullYear()}-${String(date.getUTCMonth()+1).padStart(2,"0")}`;
};
let accessToken = null;
async function request(path, method = "GET", body, authenticated = false) {
  const headers = { "Content-Type": "application/json" };
  if (authenticated && accessToken) headers.Authorization = `Bearer ${accessToken}`;
  const response = await fetch(API + path, {
    method, headers,
    body: body === undefined || body === null ? undefined : JSON.stringify(body),
  });
  if (response.status === 401 && authenticated) accessToken = null;
  if (!response.ok) throw Error(response.status === 401 ? "Session expired or invalid credentials" :
    response.status === 403 ? "You do not have permission for this action" : `Request failed (${response.status})`);
  return response.status === 204 ? null : response.json();
}

function App() {
  const [selectedMember, setSelectedMember] = useState(null),
    [privateMembers, setPrivateMembers] = useState([]),
    [data, setData] = useState(null),
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
  const can = (permission) => !!auth?.permissions?.includes(permission);
  async function signIn(e) {
    e.preventDefault();
    try {
      const session = await request("/api/auth/login", "POST", login);
      accessToken = session.accessToken;
      setAuth(session);
      if (session.permissions?.includes("MEMBER_VIEW")) {
        setPrivateMembers(await request("/api/admin/members", "GET", undefined, true));
      }
      setLogin({ email: "", password: "" });
      setAdmin(true);
      setError("");
    } catch (e) {
      setError(e.message);
    }
  }
  async function save(path, body, method = "POST") {
    try {
      await request("/api/admin" + path, method, body, true);
      await refresh();
      if (accessToken && auth?.permissions?.includes("MEMBER_VIEW")) {
        setPrivateMembers(await request("/api/admin/members", "GET", undefined, true));
      }
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
  const { contributions, loans, repayments, settings } = data;
  const members = auth && can("MEMBER_VIEW") ? privateMembers : data.members;
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
                  <th>Mobile</th>
                  <th>Shares</th>
                  <th>Monthly due</th>
                  <th>Status</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {members.map((m) => (
                  <tr key={m.id}>
                    <td>{m.fullName}</td>
                    <td>{m.mobileNumber || "Not set"}</td>
                    <td>{m.shares}</td>
                    <td>{rupee(m.shares * settings.monthlyAmountPerShare)}</td>
                    <td>{m.active ? "Active" : "Inactive"}</td>
                    <td>{auth && can("MEMBER_VIEW") && <button onClick={() => setSelectedMember(m.id)}>View / Edit</button>}</td>
                  </tr>
                ))}
              </tbody>
            </table>

          </section>
        )}
        {selectedMember !== null && auth && (
          <div className="member-modal-backdrop" onClick={() => setSelectedMember(null)}>
            <div className="member-modal" role="dialog" aria-modal="true" aria-label="Member details" onClick={e => e.stopPropagation()}>
              <MemberDetails key={selectedMember}
                member={members.find(m => Number(m.id) === Number(selectedMember))}
                roles={auth.roles || []} can={can} save={save} refresh={refresh}
                onClose={() => setSelectedMember(null)} />
            </div>
          </div>
        )}
        {tab === "contributions" && (
          <section className="panel">
            <h2>Contribution ledger</h2>
            <p className="muted">One row per member, showing the total of all active monthly contributions. Open Members → View/Edit to see individual payments.</p>
            <table>
              <thead>
                <tr>
                  <th>Member</th>
                  <th>Total contribution</th>
                </tr>
              </thead>
              <tbody>
                {Object.values(contributions.reduce((totals, c) => {
                  const id = String(c.memberId);
                  if (!totals[id]) totals[id] = { memberId: c.memberId, total: 0 };
                  totals[id].total += Number(c.amount) || 0;
                  return totals;
                }, {}))
                  .sort((a, b) => memberName(a.memberId).localeCompare(memberName(b.memberId)))
                  .map(({ memberId, total }) => (
                    <tr key={memberId}>
                      <td>{memberName(memberId)}</td>
                      <td>{rupee(total)}</td>
                    </tr>
                  ))}
                {contributions.length === 0 && (
                  <tr><td colSpan={2}>No contributions recorded yet.</td></tr>
                )}
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
                  onClick={async () => {
                    try { await request("/api/auth/logout", "POST", undefined, true); } catch {}
                    accessToken = null;
                    setAuth(null);
                    setPrivateMembers([]);
                    setTab("overview");
                    setAdmin(false);
                  }}
                >
                  Logout
                </button>
              </div>
              <AdminForms members={members} loans={loans} settings={settings} save={save} can={can} roles={auth?.roles || []} />
              {(can("USER_VIEW") || can("ROLE_VIEW")) && <AccessManagement auth={auth} can={can} />}
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

function MemberDetails({ member, roles = [], can, save, refresh, onClose }) {
  const [contributions, setContributions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [profile, setProfile] = useState({
    fullName: member?.fullName || "", mobileNumber: member?.mobileNumber || "",
    shares: member?.shares || 1, active: member?.active ?? true,
  });
  const [showAddContribution, setShowAddContribution] = useState(false);
  const [newContribution, setNewContribution] = useState({ contributionDate: today(), amount: "" });
  const [savingContribution, setSavingContribution] = useState(false);
  const canAddContribution = can("CONTRIBUTION_CREATE") &&
    (roles.includes("ADMIN") || roles.includes("SUPER_ADMIN"));
  const submitNewContribution = async e => {
    e.preventDefault();
    if (!canAddContribution || savingContribution) return;
    if (!/^\d{4}-\d{2}-\d{2}$/.test(newContribution.contributionDate) ||
        Number.isNaN(Date.parse(newContribution.contributionDate))) {
      setMessage("Select a valid contribution date.");
      return;
    }
    if (!Number.isFinite(Number(newContribution.amount)) || Number(newContribution.amount) <= 0) {
      setMessage("Enter an amount greater than zero.");
      return;
    }
    setSavingContribution(true);
    try {
      await request("/api/admin/contributions", "POST", {
        memberId: Number(member.id),
        contributionMonth: newContribution.contributionDate.slice(0, 7),
        amount: Number(newContribution.amount),
        paidOn: newContribution.contributionDate,
      }, true);
      setShowAddContribution(false);
      setNewContribution({ contributionDate: today(), amount: "" });
      await refresh();
      await loadHistory();
      setMessage("Monthly contribution added successfully.");
    } catch (e) { setMessage(e.message); }
    finally { setSavingContribution(false); }
  };
  const canDeleteContribution = can("CONTRIBUTION_DELETE") &&
    (roles.includes("ADMIN") || roles.includes("SUPER_ADMIN"));
  const [deletingContribution, setDeletingContribution] = useState(null);
  const deleteContribution = async c => {
    if (!canDeleteContribution || deletingContribution !== null) return;
    if (!window.confirm(`Delete the ${c.contributionMonth} contribution of ${rupee(c.amount)} for ${member.fullName}? This will remove it from active records and totals.`)) return;
    setDeletingContribution(c.id);
    try {
      await request(`/api/admin/contributions/${c.id}`, "DELETE", undefined, true);
      if (editing === c.id) setEditing(null);
      await refresh();
      await loadHistory();
      setMessage("Monthly contribution deleted successfully.");
    } catch (e) { setMessage(e.message); }
    finally { setDeletingContribution(null); }
  };
  const [editing, setEditing] = useState(null);
  const [correction, setCorrection] = useState({ contributionMonth: "", amount: "", reason: "" });
  const [changes, setChanges] = useState([]);
  const [message, setMessage] = useState("");
  const loadHistory = async () => {
    if (!member || !can("CONTRIBUTION_VIEW")) {
      setLoading(false);
      return;
    }
    setLoading(true);
    try {
      const [records, audit] = await Promise.all([
        request(`/api/admin/members/${member.id}/contributions`, "GET", undefined, true),
        request(`/api/admin/members/${member.id}/contribution-changes`, "GET", undefined, true)
      ]);
      setContributions(records);
      setChanges(audit);
      setMessage("");
    } catch (e) { setMessage(e.message); }
    finally { setLoading(false); }
  };
  useEffect(() => { loadHistory(); }, [member?.id]);
  if (!member) return null;
  const startEdit = c => {
    setEditing(c.id);
    setCorrection({ contributionMonth: c.contributionMonth, amount: String(c.amount), reason: "" });
  };
  const submitCorrection = async e => {
    e.preventDefault();
    try {
      await request(`/api/admin/contributions/${editing}`, "PUT", {
        ...correction, amount: Number(correction.amount)
      }, true);
      setEditing(null);
      setMessage("Contribution updated successfully");
      await refresh();
      await loadHistory();
    } catch (e) { setMessage(e.message); }
  };
  return <section className="panel">
    <h3>Member details: {member.fullName}</h3>
    <button onClick={onClose}>Close</button>
    {message && <p role="status">{message}</p>}
    <div className="fields">
      <label>Name<input value={profile.fullName} disabled={!can("MEMBER_UPDATE")}
        onChange={e => setProfile({...profile, fullName:e.target.value})}/></label>
      <label>Mobile number<input value={profile.mobileNumber} disabled={!can("MEMBER_UPDATE")}
        onChange={e => setProfile({...profile, mobileNumber:e.target.value})}/></label>
      <label>Shares<input type="number" min="1" value={profile.shares} disabled={!can("MEMBER_UPDATE")}
        onChange={e => setProfile({...profile, shares:e.target.value})}/></label>
      <label>Active<input type="checkbox" checked={profile.active} disabled={!can("MEMBER_UPDATE")}
        onChange={e => setProfile({...profile, active:e.target.checked})}/></label>
    </div>
    {can("MEMBER_UPDATE") && <button onClick={() => save(`/members/${member.id}`, {...profile, shares:Number(profile.shares)}, "PUT")}>Save member</button>}
    <div className="member-contribution-heading">
      <h3>Monthly contribution history</h3>
      {canAddContribution && <button type="button" onClick={() => { setShowAddContribution(v => !v); setMessage(""); }}>
        {showAddContribution ? "Cancel adding" : "+ Add monthly record"}
      </button>}
    </div>
    {showAddContribution && canAddContribution && <form className="member-add-contribution-card" onSubmit={submitNewContribution}>
      <h4>Add monthly contribution</h4>
      <div className="fields">
        <label>Member name
          <input value={member.fullName} readOnly aria-label="Member name" />
        </label>
        <label>Date (day / month / year)
          <input type="date" required
            value={newContribution.contributionDate}
            onChange={e => setNewContribution({...newContribution, contributionDate:e.target.value})} />
        </label>
        <label>Amount (₹)
          <input type="number" min="0.01" step="0.01" required placeholder="Enter amount"
            value={newContribution.amount}
            onChange={e => setNewContribution({...newContribution, amount:e.target.value})} />
        </label>
      </div>
      <p className="member-add-contribution-note">Choose any valid date. The contribution month and payment date are saved from the selected date.</p>
      <div className="member-contribution-actions">
        <button type="submit" disabled={savingContribution}>{savingContribution ? "Saving..." : "Save monthly record"}</button>
        <button type="button" onClick={() => setShowAddContribution(false)} disabled={savingContribution}>Cancel</button>
      </div>
    </form>}

    {loading && <p>Loading contribution history...</p>}
    {!loading && contributions.length === 0 && <p>No contributions found for this member.</p>}
    <table><thead><tr><th>Month</th><th>Amount</th><th>Paid on</th><th>Action</th></tr></thead>
      <tbody>{[...contributions].sort((a,b) => b.contributionMonth.localeCompare(a.contributionMonth)).map(c =>
        <tr key={c.id}><td>{c.contributionMonth}</td><td>{rupee(c.amount)}</td><td>{c.paidOn}</td>
          <td><div className="member-contribution-actions">
            {can("CONTRIBUTION_UPDATE") && <button type="button" onClick={() => startEdit(c)}>Edit</button>}
            {canDeleteContribution && <button type="button" className="contribution-delete-button"
              disabled={deletingContribution !== null} onClick={() => deleteContribution(c)}>
              {deletingContribution === c.id ? "Deleting..." : "Delete"}
            </button>}
          </div></td></tr>
      )}</tbody>
    </table>
    {editing !== null && <form onSubmit={submitCorrection}>
      <h4>Edit contribution #{editing}</h4>
      <label>Month<input type="month" required value={correction.contributionMonth}
        onChange={e => setCorrection({...correction, contributionMonth:e.target.value})}/></label>
      <label>Amount<input type="number" min="0.01" step="0.01" required value={correction.amount}
        onChange={e => setCorrection({...correction, amount:e.target.value})}/></label>
      <label>Reason for change<input required maxLength={500} value={correction.reason}
        onChange={e => setCorrection({...correction, reason:e.target.value})}/></label>
      <button type="submit">Save correction</button><button type="button" onClick={() => setEditing(null)}>Cancel</button>
    </form>}
    <h3>Contribution correction audit</h3>
    <table><thead><tr><th>Changed at</th><th>Month (old → new)</th><th>Amount (old → new)</th><th>Admin</th><th>Reason</th></tr></thead>
      <tbody>{changes.map(change => <tr key={change.id}><td>{change.changedAt}</td>
        <td>{change.oldMonth} → {change.newMonth}</td>
        <td>{rupee(change.oldAmount)} → {rupee(change.newAmount)}</td>
        <td>{change.changedBy}</td><td>{change.reason}</td></tr>)}</tbody>
    </table>
  </section>;
}

function AdminForms({ members, loans, settings, save, can, roles }) {
  const [member, setMember] = useState({ fullName: "", mobileNumber: "", shares: 1 }),
    [contribution, setContribution] = useState({
      memberId: "",
      contributionMonth: monthOffset(0),
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
      {can("MEMBER_CREATE") && form(
        "New member",
        [
          ["fullName", "Full name", "text"],
          ["mobileNumber", "Mobile number (10 digits)", "tel"],
          ["shares", "Shares", "number"],
        ],
        member,
        setMember,
        () => save("/members", { ...member, shares: Number(member.shares) }),
      )}
      {can("CONTRIBUTION_CREATE") && roles.some(r => r === "ADMIN" || r === "SUPER_ADMIN") && form(
        "Record contribution",
        [
          ["memberId", "Member", "select", members.filter((m) => m.active)],
          ["contributionMonth", "Month", "month"],
          ["amount", "Amount ₹", "number"],
          ["paidOn", "Payment date", "date"],
        ],
        contribution,
        setContribution,
        () => save("/contributions", {
          ...contribution,
          memberId: Number(contribution.memberId),
          amount: Number(contribution.amount),
        }),
      )}
      {can("LOAN_CREATE") && form(
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
      {can("LOAN_REPAYMENT_CREATE") && form(
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
      {can("SETTINGS_UPDATE") && form(
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
function AccessManagement({ auth, can }) {
  const [users, setUsers] = useState([]), [roles, setRoles] = useState([]),
    [permissions, setPermissions] = useState([]), [error, setError] = useState(""),
    [newUser, setNewUser] = useState({ fullName: "", email: "", mobileNumber: "", password: "", roleIds: [] }),
    [newRole, setNewRole] = useState({ name: "", description: "", active: true });
  const load = async () => {
    try {
      if (can("USER_VIEW")) setUsers(await request("/api/admin/users", "GET", null, true));
      if (can("ROLE_VIEW")) {
        setRoles(await request("/api/admin/roles", "GET", null, true));
        setPermissions(await request("/api/admin/permissions", "GET", null, true));
      }
      setError("");
    } catch (e) { setError(e.message); }
  };
  useEffect(() => { load(); }, []);
  const action = async (path, method, body) => {
    try { await request(path, method, body, true); await load(); }
    catch (e) { setError(e.message); }
  };
  const updateUserRoles = async (u, roleId, checked) => {
    const ids = roles.filter(r => u.roles.includes(r.name)).map(r => r.id);
    await action(`/api/admin/users/${u.id}/roles`, "PUT", { ids: checked ? [...new Set([...ids, roleId])] : ids.filter(id => id !== roleId) });
  };
  return <div className="admin-grid">
    {error && <p className="error">{error}</p>}
    {can("USER_VIEW") && <section className="panel">
      <h2>Manage users</h2>
      <div className="users-table-scroll"><table className="users-table">
        <thead><tr><th>User</th><th>Assigned roles</th><th>Status</th><th>Actions</th></tr></thead>
        <tbody>{users.map(u => <tr key={u.id}>
          <td className="user-identity"><strong>{u.fullName || u.email}</strong><small>{u.email}</small><small>{u.mobileNumber || "Mobile not set"}</small></td>
          <td><div className="user-role-cell">
            <div className="user-role-badges">{u.roles.map(role => <span className="user-role-badge" key={role}>{role}</span>)}</div>
            {can("ROLE_MANAGE") && <div className="user-role-options">{roles.map(r => <label className="user-role-option" key={r.id}>
              <input type="checkbox" checked={u.roles.includes(r.name)} onChange={e => updateUserRoles(u, r.id, e.target.checked)} />
              <span>{r.name}</span>
            </label>)}</div>}
          </div></td>
          <td><span className={u.enabled ? "user-status active" : "user-status disabled"}>{u.enabled ? "Active" : "Disabled"}</span></td>
          <td><div className="user-row-actions">
            {can("USER_DISABLE") && <button type="button" onClick={() => action(`/api/admin/users/${u.id}/status`,"PATCH",{enabled:!u.enabled})}>{u.enabled ? "Disable" : "Enable"}</button>}
            {can("USER_UPDATE") && <button className="user-reset-button" type="button" onClick={() => {const password=prompt("Enter new temporary password (12+ characters)"); if(password)action(`/api/admin/users/${u.id}/reset-password`,"POST",{password});}}>Reset password</button>}
          </div></td>
        </tr>)}</tbody>
      </table></div>
      {can("USER_CREATE") && <form onSubmit={async e => {e.preventDefault();await action("/api/admin/users","POST",newUser);setNewUser({fullName:"",email:"",mobileNumber:"",password:"",roleIds:[]});}}>
        <h3>Create administrator</h3>
        <label>Full name<input required value={newUser.fullName} onChange={e=>setNewUser({...newUser,fullName:e.target.value})}/></label>
        <label>Email<input required type="email" value={newUser.email} onChange={e=>setNewUser({...newUser,email:e.target.value})}/></label>
        <label>Mobile number<input required type="tel" inputMode="numeric" pattern="[6-9][0-9]{9}" maxLength={10} placeholder="10-digit mobile number" value={newUser.mobileNumber} onChange={e=>setNewUser({...newUser,mobileNumber:e.target.value})}/></label>
        <label>Temporary password<input required minLength={12} type="password" autoComplete="new-password" value={newUser.password} onChange={e=>setNewUser({...newUser,password:e.target.value})}/></label>
        <label>Role<select required value={newUser.roleIds[0] || ""} onChange={e=>setNewUser({...newUser,roleIds:[Number(e.target.value)]})}><option value="">Select role</option>{roles.map(r=><option key={r.id} value={r.id}>{r.name}</option>)}</select></label>
        <button>Create user</button>
      </form>}
    </section>}
    {can("ROLE_VIEW") && <section className="panel"><h2>Roles and permissions</h2>
      {roles.map(r=><div key={r.id}><h3>{r.name}</h3><div className="fields">{permissions.map(p=><label key={p.id}>
        <input type="checkbox" disabled={!can("ROLE_MANAGE") || r.name==="SUPER_ADMIN"} checked={r.permissionIds.includes(p.id)} onChange={e=>action(`/api/admin/roles/${r.id}/permissions`,"PUT",{ids:e.target.checked?[...r.permissionIds,p.id]:r.permissionIds.filter(id=>id!==p.id)})}/> {p.code}
      </label>)}</div></div>)}
      {can("ROLE_MANAGE") && <form onSubmit={async e=>{e.preventDefault();await action("/api/admin/roles","POST",newRole);setNewRole({name:"",description:"",active:true});}}>
        <h3>Create role</h3><label>Name<input required value={newRole.name} onChange={e=>setNewRole({...newRole,name:e.target.value})}/></label>
        <label>Description<input value={newRole.description} onChange={e=>setNewRole({...newRole,description:e.target.value})}/></label>
        <button>Create role</button>
      </form>}
    </section>}
  </div>;
}
createRoot(document.getElementById("root")).render(<App />);
