// Dashboard — KPI overview (spec rule 29). Values are wired to backend
// analytics endpoints in Phase 10/11; shown here as the layout + labels.

const kpis: { label: string; hint: string }[] = [
  { label: "کاربران کل", hint: "Total Users" },
  { label: "کاربران فعال", hint: "DAU / WAU / MAU" },
  { label: "کاربران پولی", hint: "Paid Users" },
  { label: "درآمد", hint: "Revenue" },
  { label: "هزینهٔ AI", hint: "AI Cost" },
  { label: "سود ناخالص", hint: "Gross Profit" },
  { label: "تعداد درخواست‌ها", hint: "Requests" },
  { label: "نرخ تبدیل", hint: "Conversion" },
];

export default function DashboardPage() {
  return (
    <div>
      <h1>داشبورد</h1>
      <p style={{ color: "#666" }}>
        شاخص‌های کلیدی محصول و مالی. اتصال به endpointهای آنالیتیکس در فاز ۱۰/۱۱.
      </p>
      <div
        style={{
          display: "grid",
          gridTemplateColumns: "repeat(auto-fill, minmax(200px, 1fr))",
          gap: 16,
          marginTop: 24,
        }}
      >
        {kpis.map((k) => (
          <div
            key={k.hint}
            style={{
              background: "#fff",
              borderRadius: 14,
              padding: 20,
              boxShadow: "0 1px 3px rgba(0,0,0,0.06)",
            }}
          >
            <div style={{ fontSize: 13, color: "#888" }}>{k.hint}</div>
            <div style={{ fontSize: 18, fontWeight: 700, marginTop: 4 }}>{k.label}</div>
            <div style={{ fontSize: 28, fontWeight: 800, marginTop: 8 }}>—</div>
          </div>
        ))}
      </div>
    </div>
  );
}
