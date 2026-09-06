import type { ReactNode } from "react";

export const metadata = {
  title: "Deep Gap — Admin",
  description: "پنل مدیریت دیپ گپ",
};

const nav = [
  { href: "/", label: "داشبورد" },
  { href: "/models", label: "مدل‌ها و قیمت" },
  { href: "/users", label: "کاربران" },
  { href: "/finance", label: "مالی" },
  { href: "/config", label: "تنظیمات و Flags" },
];

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="fa" dir="rtl">
      <body style={{ margin: 0, fontFamily: "system-ui, sans-serif", background: "#f6f7fb" }}>
        <div style={{ display: "flex", minHeight: "100vh" }}>
          <aside
            style={{
              width: 220,
              background: "#1b1f3b",
              color: "#fff",
              padding: 20,
            }}
          >
            <h2 style={{ marginTop: 0 }}>دیپ گپ</h2>
            <nav style={{ display: "flex", flexDirection: "column", gap: 10 }}>
              {nav.map((n) => (
                <a key={n.href} href={n.href} style={{ color: "#cfd3ff", textDecoration: "none" }}>
                  {n.label}
                </a>
              ))}
            </nav>
          </aside>
          <main style={{ flex: 1, padding: 32 }}>{children}</main>
        </div>
      </body>
    </html>
  );
}
