"""
DIAG-02 — System Architecture Overview
Two-part output:
  diag-02a-containers.png  — C4 Level-2 container view  (16x9, 200 DPI)
  diag-02b-monolith.png    — Monolith domain package map (16x11, 200 DPI)
"""

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
from matplotlib.patches import FancyBboxPatch

BASE = (
    r"c:\Users\twin\OneDrive - Netcompany\Documents\DWP"
    r"\DWP-system-prototype\docs\presentations"
)

# ── shared helpers ─────────────────────────────────────────────────────────────

def make_fig(w=16, h=9):
    fig, ax = plt.subplots(figsize=(w, h), dpi=200)
    ax.set_xlim(0, w)
    ax.set_ylim(0, h)
    ax.axis("off")
    fig.patch.set_facecolor("#FFFFFF")
    return fig, ax

def rbox(ax, x, y, w, h, title, body="",
         hdr_clr="#1565C0", bdy_clr="#FFFFFF",
         hdr_txt="#FFFFFF", bdy_txt="#1A1A1A",
         r=0.14, z=3, title_fs=10.0, body_fs=8.0):
    hdr_h = min(h * 0.38, 0.50)
    ax.add_patch(FancyBboxPatch((x, y), w, h,
                                boxstyle=f"round,pad=0,rounding_size={r}",
                                fc=bdy_clr, ec=hdr_clr, lw=1.8, zorder=z))
    ax.add_patch(FancyBboxPatch((x, y + h - hdr_h), w, hdr_h,
                                boxstyle=f"round,pad=0,rounding_size={r}",
                                fc=hdr_clr, ec="none", lw=0, zorder=z + 1))
    ax.add_patch(mpatches.Rectangle((x, y + h - hdr_h), w, hdr_h * 0.5,
                                    fc=hdr_clr, ec="none", lw=0, zorder=z + 1))
    ax.text(x + w / 2, y + h - hdr_h / 2, title,
            ha="center", va="center", fontsize=title_fs, fontweight="bold",
            color=hdr_txt, zorder=z + 2, clip_on=False)
    if body:
        ax.text(x + w / 2, y + (h - hdr_h) / 2, body,
                ha="center", va="center", fontsize=body_fs, color=bdy_txt,
                multialignment="center", linespacing=1.35, zorder=z + 2,
                clip_on=False)

def brect(ax, x, y, w, h, label, fill, border, lbl_clr, z=1, lbl_fs=10.0):
    ax.add_patch(FancyBboxPatch((x, y), w, h,
                                boxstyle="round,pad=0,rounding_size=0.22",
                                fc=fill, ec=border, lw=2.2, zorder=z, alpha=0.45))
    ax.text(x + 0.18, y + h - 0.18, label,
            ha="left", va="top", fontsize=lbl_fs, fontweight="bold",
            color=lbl_clr, zorder=z + 1, clip_on=False)

def arr(ax, x0, y0, x1, y1, lbl="", clr="#263238",
        lbl_x=None, lbl_y=None, lbl_ha="center",
        z=8, bi=False, dashed=False, lbl_fs=7.5):
    style = "<|-|>" if bi else "-|>"
    ls = "dashed" if dashed else "solid"
    ax.annotate("", xy=(x1, y1), xytext=(x0, y0),
                arrowprops=dict(arrowstyle=style, color=clr, lw=1.8,
                                linestyle=ls, mutation_scale=12),
                zorder=z)
    if lbl:
        lx = lbl_x if lbl_x is not None else (x0 + x1) / 2
        ly = lbl_y if lbl_y is not None else (y0 + y1) / 2
        ax.text(lx, ly, lbl, ha=lbl_ha, va="center", fontsize=lbl_fs,
                color=clr, style="italic",
                bbox=dict(boxstyle="round,pad=0.12", fc="white", ec="none", alpha=0.93),
                zorder=z + 1, clip_on=False)


# ══════════════════════════════════════════════════════════════════════════════
# DIAGRAM A — C4 Container view  (16 × 9)
#
# Column layout (x coordinates):
#   0.12 – 2.05  Users
#   2.20 – 4.65  DCMS infra (Frontend / Database / Keycloak)  <- inside DCMS boundary
#   4.80 – 11.45 Backend Monolith                             <- inside DCMS boundary
#  11.55 – 15.88 External systems boundary
#
# Connector strategy:
#   - All horizontal arrows between adjacent columns only
#   - Right-side labels sit ABOVE each arrow, in the gap between monolith and
#     external boundary (x ~11.45–11.55), rotated or kept very short
#   - Vertical arrow (Frontend -> Keycloak) runs in infra column with label
#     placed to the right of the arrow stem
# ══════════════════════════════════════════════════════════════════════════════

fig, ax = make_fig(16, 9)

C_SVC = "#1565C0"
C_INF = "#2E7D32"
C_EXT = "#5D4037"
C_USR = "#37474F"
C_ARR = "#263238"

ax.text(8.0, 8.83, "DCMS — System Architecture  (C4 Container Level)",
        ha="center", va="top", fontsize=14, fontweight="bold", color="#1A1A1A")

# Users
rbox(ax, 0.12, 7.00, 1.93, 1.50, "DWP Staff",
     "Agents / Supervisors\nBusiness Admins",
     C_USR, bdy_clr="#ECEFF1", bdy_txt="#1A1A1A")
rbox(ax, 0.12, 5.10, 1.93, 1.50, "DWP Customer",
     "Self-service portal\n(future)",
     C_USR, bdy_clr="#ECEFF1", bdy_txt="#1A1A1A")

# DCMS boundary — spans infra column + monolith
brect(ax, 2.10, 0.30, 9.40, 8.28,
      "DCMS  —  Debt Collection Management System",
      "#EBF3FB", "#1565C0", "#0D47A1", lbl_fs=10.0)

# Infra column boxes
rbox(ax, 2.28, 7.00, 2.28, 1.50, "Frontend",
     "React + TypeScript\nNginx 1.27-alpine\nGOV.UK Design System",
     C_SVC, title_fs=10.0, body_fs=8.5)

rbox(ax, 2.28, 4.90, 2.28, 1.65, "Database",
     "PostgreSQL 16\npublic schema (app)\nflowable schema (engine)",
     C_INF, title_fs=10.0, body_fs=8.5)

rbox(ax, 2.28, 2.95, 2.28, 1.65, "Identity Provider",
     "Keycloak 24\nOAuth 2.0 + OIDC\nRBAC via JWT claims",
     C_INF, title_fs=10.0, body_fs=8.5)

# Backend Monolith — wide single box, text describes interior without nesting
rbox(ax, 4.68, 0.58, 6.68, 7.90,
     "Backend Monolith  —  Spring Boot 3.4 / Java 21",
     (
         "Domain packages\n"
         "customer   account   strategy   workallocation\n"
         "repaymentplan   payment   communications   integration\n"
         "audit   analytics   reporting   user   thirdpartymanagement\n"
         "\n"
         "Process engine isolation  (ADR-003)\n"
         "Domain modules\n"
         "  call shared/process/port interfaces only  [zero Flowable imports]\n"
         "shared/process/port is implemented by infrastructure/process\n"
         "  [all Flowable imports confined to infrastructure/process]\n"
         "infrastructure/process drives Flowable BPMN/DMN Engine\n"
         "\n"
         "See companion diagram for package-level detail"
     ),
     C_SVC, bdy_clr="#EEF4FF", bdy_txt="#1A1A1A",
     title_fs=11.0, body_fs=9.0)

# External boundary — starts after monolith right edge
brect(ax, 11.60, 0.30, 4.20, 8.28,
      "External Systems",
      "#FFF8F0", "#5D4037", "#4E342E", lbl_fs=10.0)

EW, EH = 3.82, 1.28
EX = 11.78
EXT_ITEMS = [
    ("DWP Place / DM6",         "Debt referral source system"),
    ("DWP Self-Service Portal",  "Customer portal\n(inbound API only)"),
    ("Payment Gateway",          "Direct debit & card\npayment processing"),
    ("Credit Reference Agency",  "Bureau scorecard\nand CRA feeds"),
    ("Debt Collection Agents",   "DCA placement\nrecall & reconciliation"),
]
EYS = [6.78, 5.30, 3.82, 2.34, 0.86]
for (t, b), ey in zip(EXT_ITEMS, EYS):
    rbox(ax, EX, ey, EW, EH, t, b,
         C_EXT, bdy_clr="#FFF3E0", bdy_txt="#1A1A1A",
         title_fs=9.5, body_fs=8.5)

# ── Arrows ─────────────────────────────────────────────────────────────────────

# Staff -> Frontend
arr(ax, 2.05, 7.75, 2.28, 7.75, "HTTPS")
# Customer -> portal (dashed, future)
arr(ax, 2.05, 5.85, 2.28, 5.85, "future", clr="#78909C", dashed=True)

# Frontend <-> Monolith  (mid-height horizontal)
arr(ax, 4.56, 7.75, 4.68, 7.75, "HTTPS / REST JSON", bi=True)

# Frontend -> Keycloak  (vertical, right side of infra column)
# Arrow runs from frontend bottom-centre to Keycloak top-centre
arr(ax, 3.42, 7.00, 3.42, 4.60,
    "OIDC login",
    lbl_x=3.85, lbl_y=5.80, lbl_ha="left")

# Monolith -> Keycloak
arr(ax, 4.68, 3.78, 4.56, 3.78, "validate JWT (JWKS)",
    lbl_x=4.62, lbl_y=3.94, lbl_ha="center")

# Monolith <-> Database
arr(ax, 4.68, 5.72, 4.56, 5.72, "JDBC / JPA", bi=True)

# Right-side: Monolith -> External systems
# Arrows go from monolith right edge (x=11.36) to external box left edge (x=11.60)
# Labels sit above each arrow line, placed just outside the monolith boundary
# to keep the monolith body clear.
# Right-side connectors: arrow starts at x=10.85 (inside monolith, below body text
# area) and ends at x=11.60 (external boundary left edge).
# Label is placed ABOVE the arrow, centred on the full span, at lbl_y = ry + 0.18.
# Using a longer arrow span gives the label enough room to render without clipping.
RIGHT_CONNS = [
    # (y_level,  label,                              bidirectional)
    (7.42, "Debt referral ingest — HTTPS/REST",       False),
    (5.94, "I&E / payments / messages — HTTPS/REST",  False),
    (4.46, "Payment instructions — HTTPS/REST",        True),
    (2.98, "Pull scorecard data — HTTPS/REST",         False),
    (1.50, "Placement & recall — REST or batch",       True),
]
for (ry, lbl, bi_flag) in RIGHT_CONNS:
    arr(ax, 10.85, ry, 11.60, ry,
        lbl,
        lbl_x=11.22, lbl_y=ry + 0.18, lbl_ha="center",
        bi=bi_flag, lbl_fs=7.5)

# Legend
LX, LY = 0.12, 4.55
ax.text(LX, LY, "Legend", fontsize=9.5, fontweight="bold", color="#1A1A1A")
for i, (c, l) in enumerate([
    (C_SVC, "Runtime service"),
    (C_INF, "Infrastructure"),
    (C_EXT, "External system"),
    (C_USR, "User / actor"),
]):
    iy = LY - 0.52 - i * 0.44
    ax.add_patch(mpatches.FancyBboxPatch((LX, iy), 0.27, 0.25,
                                          boxstyle="round,pad=0.03", fc=c, ec="none"))
    ax.text(LX + 0.37, iy + 0.125, l, va="center", fontsize=8.0, color="#1A1A1A")

ax.text(8.0, 0.08,
        "All external integrations routed through domain/integration anti-corruption layer   |   "
        "Flowable is embedded inside the monolith JVM   |   "
        "See companion diagram for domain package detail",
        ha="center", va="bottom", fontsize=7.2, color="#546E7A", style="italic")

fig.savefig(f"{BASE}\\diag-02a-containers.png",
            dpi=200, bbox_inches="tight", facecolor="white")
print("Saved: diag-02a-containers.png")
plt.close(fig)


# ══════════════════════════════════════════════════════════════════════════════
# DIAGRAM B — Monolith interior  (16 × 11 for extra vertical room)
#
# Layout:
#   x 0.45 – 8.25  : domain package grid (3 col) + full-width rows
#   x 8.60 – 15.55 : callout annotations (right column)
#   Vertical arrows run along spine at x = MX (centre of left column)
# ══════════════════════════════════════════════════════════════════════════════

fig, ax = make_fig(16, 11)

C_DOM = "#283593"
C_PRT = "#6A1B9A"
C_PRC = "#AD1457"
C_FLW = "#B71C1C"
C_DB  = "#2E7D32"

ax.text(8.0, 10.83,
        "DCMS Backend Monolith — Domain Packages & Process Engine Isolation",
        ha="center", va="top", fontsize=14, fontweight="bold", color="#1A1A1A")

# Grid geometry — sized so body text fits comfortably
PW = 2.50   # package box width
PH = 1.12   # package box height  (enough for 2-line body at 8pt)
GX = 0.13   # horizontal gap
GY = 0.14   # vertical gap

SX    = 0.45    # grid left edge
SY_T  = 10.42   # y of top edge of first row

TW = 3 * PW + 2 * GX   # 7.76 — total grid width

PKGS = [
    # col 0           col 1               col 2
    ("customer",      "Identity\nVulnerability flags · Joint liability\nThird-party authority"),
    ("account",       "Financial ledger\nRegulatory state: breathing space\ninsolvency · deceased · fraud"),
    ("strategy",      "Decisioning\nTreatment routing\nChampion/challenger"),

    ("workallocation","Queues · Worklists\nAgent assignment\nSupervisor override"),
    ("repaymentplan", "Arrangements\nI&E capture · Schedule mgmt\nBreach handling"),
    ("payment",       "Payment posting\nAllocation logic\nFinancial events · Reconciliation"),

    ("communications","Contact history\nCorrespondence\nChannel suppression rules"),
    ("integration",   "Anti-corruption layer\nInbound/outbound APIs\nBatch file transfer"),
    ("audit",         "Immutable event log\nCompliance evidence\nTrace records"),

    ("analytics",     "Scoring models\nSegmentation\nBureau scorecard · DMN tables"),
    ("reporting",     "MI exports\nKPI read models"),
    ("user",          "RBAC\nUser management\nKeycloak integration"),
]

for i, (name, body) in enumerate(PKGS):
    col, row = i % 3, i // 3
    px = SX + col * (PW + GX)
    py = SY_T - (row + 1) * (PH + GY)
    rbox(ax, px, py, PW, PH, name, body,
         C_DOM, bdy_clr="#EEF2FF", bdy_txt="#1A1A1A",
         title_fs=9.5, body_fs=8.0)

# Full-width rows
def fry(n):   # n=0 immediately below row 3, etc.
    return SY_T - (4 + n) * (PH + GY) - PH

FH = PH  # same height for full rows

rbox(ax, SX, fry(0), TW, FH,
     "thirdpartymanagement",
     "DCA Placement  ·  Recall  ·  Commission / Billing  ·  Third-party partner interfaces",
     C_DOM, bdy_clr="#EEF2FF", bdy_txt="#1A1A1A", title_fs=9.5, body_fs=8.5)

rbox(ax, SX, fry(1), TW, FH,
     "shared / process / port",
     "ProcessEventPort  ·  ProcessStartPort  ·  DelegateCommandBus\n"
     "DelegateCommand  ·  DelegateCommandHandler\n"
     "ZERO Flowable imports — domain modules interact with the process engine only through these interfaces",
     C_PRT, bdy_clr="#F3E5F5", bdy_txt="#1A1A1A", title_fs=9.5, body_fs=8.0)

rbox(ax, SX, fry(2), TW, FH,
     "infrastructure / process",
     "Flowable engine config  ·  BPMN/DMN model resources  ·  JavaDelegate implementations\n"
     "ProcessEventPort / ProcessStartPort implementations\n"
     "ALL Flowable imports confined here — no other package may import Flowable types",
     C_PRC, bdy_clr="#FCE4EC", bdy_txt="#1A1A1A", title_fs=9.5, body_fs=8.0)

# Flowable engine + PostgreSQL below
FLY  = fry(3)
FLH  = PH * 0.80
FLW  = TW * 0.54
FLX  = SX + (TW - FLW) / 2
rbox(ax, FLX, FLY, FLW, FLH,
     "Flowable BPMN/DMN Engine",
     "Embedded in monolith JVM\nManages own transaction on flowable schema",
     C_FLW, bdy_clr="#FFEBEE", bdy_txt="#1A1A1A", title_fs=9.5, body_fs=8.0)

DBH = 0.55
DBW = TW * 0.42
DBX = SX + (TW - DBW) / 2
DBY = FLY - DBH - 0.14
rbox(ax, DBX, DBY, DBW, DBH,
     "PostgreSQL 16  —  flowable schema", "",
     C_DB, bdy_clr="#E8F5E9", bdy_txt="#1A1A1A",
     title_fs=8.5, body_fs=7.5, r=0.10)

# ── Vertical spine arrows ──────────────────────────────────────────────────────
MX = SX + TW / 2

def spine_arr(ax, from_y, to_y, lbl, clr):
    arr(ax, MX, from_y, MX, to_y, lbl, clr,
        lbl_x=MX + 1.55, lbl_y=(from_y + to_y) / 2,
        lbl_ha="left", lbl_fs=8.0)

spine_arr(ax, fry(0),        fry(1) + FH,  "domain calls port interfaces only",    C_PRT)
spine_arr(ax, fry(1),        fry(2) + FH,  "implemented by",                       C_PRC)
spine_arr(ax, fry(2),        FLY + FLH,    "RuntimeService / TaskService",          C_FLW)
spine_arr(ax, FLY,           DBY + DBH,    "JDBC (Flowable-internal)",              C_DB)

# ── Right column callouts ──────────────────────────────────────────────────────
RX = SX + TW + 0.55

def note(ax, x, y, text, clr):
    ax.text(x, y, text, ha="left", va="center", fontsize=8.5, color=clr,
            bbox=dict(boxstyle="round,pad=0.4", fc="white", ec=clr, lw=1.6),
            clip_on=False)

note(ax, RX, SY_T - 2 * (PH + GY),
     "13 domain packages.\nNo Flowable imports anywhere.\nAll external calls via\ndomain/integration ACL.",
     C_DOM)

note(ax, RX, fry(0) + FH / 2,
     "Process engine\nisolation boundary begins here.",
     C_PRT)

note(ax, RX, fry(1) + FH / 2,
     "Zero Flowable imports.\nDomain code is fully testable\nwithout a running engine.",
     C_PRT)

note(ax, RX, fry(2) + FH / 2,
     "All Flowable code lives here.\nFlowable calls are always OUTSIDE\n@Transactional  (ADR-003).",
     C_PRC)

note(ax, RX, FLY + FLH / 2,
     "One process instance\nper DebtAccount.",
     C_FLW)

ax.text(8.0, 0.06,
        "App DB writes always @Transactional — Flowable engine calls always outside transaction boundary (ADR-003).",
        ha="center", va="bottom", fontsize=7.8, color="#546E7A", style="italic")

fig.savefig(f"{BASE}\\diag-02b-monolith.png",
            dpi=200, bbox_inches="tight", facecolor="white")
print("Saved: diag-02b-monolith.png")
plt.close(fig)
