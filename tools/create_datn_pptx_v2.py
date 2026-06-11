from __future__ import annotations

import math
import os
import zipfile
from pathlib import Path
from xml.sax.saxutils import escape

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(r"D:\Documents\Workspace\traceability-system")
ASSETS = ROOT / "slide_assets"
ICON_DIR = ASSETS / "icons_v2"
OUT = ROOT / "DATN_Nguyen_Dang_Nam_Traceability_Blockchain_v2.pptx"

EMU = 914400
SLIDE_W_IN = 13.333333
SLIDE_H_IN = 7.5
SLIDE_W = int(SLIDE_W_IN * EMU)
SLIDE_H = int(SLIDE_H_IN * EMU)

C = {
    "bg": "F7FAFC",
    "paper": "FFFFFF",
    "ink": "0B1220",
    "text": "1F2937",
    "muted": "64748B",
    "line": "D8E0EA",
    "blue": "2563EB",
    "blue_dark": "1D4ED8",
    "blue_soft": "EAF2FF",
    "green": "16A34A",
    "green_dark": "15803D",
    "green_soft": "EAFBF0",
    "orange": "F97316",
    "orange_soft": "FFF3E8",
    "red": "DC2626",
    "red_soft": "FFF0F0",
    "slate_soft": "EEF3F8",
    "dark": "0F172A",
}


def emu(v: float) -> int:
    return int(v * EMU)


def xesc(s: str) -> str:
    return escape(str(s), {'"': "&quot;"})


def fill(color: str) -> str:
    return f'<a:solidFill><a:srgbClr val="{color}"/></a:solidFill>'


def ln(color: str = "D8E0EA", width: int = 9525, alpha: int | None = None) -> str:
    a = f"<a:alpha val=\"{alpha}\"/>" if alpha else ""
    return f'<a:ln w="{width}"><a:solidFill><a:srgbClr val="{color}">{a}</a:srgbClr></a:solidFill></a:ln>'


def no_line() -> str:
    return "<a:ln><a:noFill/></a:ln>"


def shadow() -> str:
    return '<a:effectLst><a:outerShdw blurRad="38100" dist="19050" dir="5400000" algn="ctr" rotWithShape="0"><a:srgbClr val="94A3B8"><a:alpha val="18000"/></a:srgbClr></a:outerShdw></a:effectLst>'


def shape(
    sid: int,
    x: float,
    y: float,
    w: float,
    h: float,
    color: str,
    line: str | None = "D8E0EA",
    prst: str = "roundRect",
    shadowed: bool = False,
) -> str:
    line_xml = no_line() if line is None else ln(line)
    return f"""
<p:sp>
  <p:nvSpPr><p:cNvPr id="{sid}" name="Shape {sid}"/><p:cNvSpPr/><p:nvPr/></p:nvSpPr>
  <p:spPr>
    <a:xfrm><a:off x="{emu(x)}" y="{emu(y)}"/><a:ext cx="{emu(w)}" cy="{emu(h)}"/></a:xfrm>
    <a:prstGeom prst="{prst}"><a:avLst/></a:prstGeom>
    {fill(color)}{line_xml}{shadow() if shadowed else ""}
  </p:spPr>
  <p:txBody><a:bodyPr/><a:lstStyle/><a:p/></p:txBody>
</p:sp>"""


def text(
    sid: int,
    x: float,
    y: float,
    w: float,
    h: float,
    value: str,
    size: int = 18,
    color: str = "1F2937",
    bold: bool = False,
    align: str = "l",
    valign: str = "top",
    font: str = "Aptos",
) -> str:
    paras = []
    for line in str(value).split("\n"):
        b = ' b="1"' if bold else ""
        paras.append(
            f'<a:p><a:pPr algn="{align}"/>'
            f'<a:r><a:rPr lang="vi-VN" sz="{size * 100}"{b}>'
            f'<a:solidFill><a:srgbClr val="{color}"/></a:solidFill>'
            f'<a:latin typeface="{font}"/></a:rPr><a:t>{xesc(line)}</a:t></a:r>'
            f'<a:endParaRPr lang="vi-VN" sz="{size * 100}"/></a:p>'
        )
    anchor = {"top": "t", "mid": "ctr", "bottom": "b"}.get(valign, "t")
    return f"""
<p:sp>
  <p:nvSpPr><p:cNvPr id="{sid}" name="Text {sid}"/><p:cNvSpPr txBox="1"/><p:nvPr/></p:nvSpPr>
  <p:spPr>
    <a:xfrm><a:off x="{emu(x)}" y="{emu(y)}"/><a:ext cx="{emu(w)}" cy="{emu(h)}"/></a:xfrm>
    <a:prstGeom prst="rect"><a:avLst/></a:prstGeom><a:noFill/>{no_line()}
  </p:spPr>
  <p:txBody><a:bodyPr wrap="square" anchor="{anchor}"/><a:lstStyle/>{''.join(paras)}</p:txBody>
</p:sp>"""


def pic(sid: int, rid: str, name: str, x: float, y: float, w: float, h: float, line_color: str = "D8E0EA") -> str:
    return f"""
<p:pic>
  <p:nvPicPr><p:cNvPr id="{sid}" name="{xesc(name)}"/><p:cNvPicPr/><p:nvPr/></p:nvPicPr>
  <p:blipFill><a:blip r:embed="{rid}"/><a:stretch><a:fillRect/></a:stretch></p:blipFill>
  <p:spPr>
    <a:xfrm><a:off x="{emu(x)}" y="{emu(y)}"/><a:ext cx="{emu(w)}" cy="{emu(h)}"/></a:xfrm>
    <a:prstGeom prst="rect"><a:avLst/></a:prstGeom>{ln(line_color)}
  </p:spPr>
</p:pic>"""


def icon(sid: int, rid: str, name: str, x: float, y: float, size: float) -> str:
    return pic(sid, rid, name, x, y, size, size, "FFFFFF")


def line_shape(sid: int, x: float, y: float, w: float, h: float, color: str = "CBD5E1") -> str:
    return shape(sid, x, y, w, h, color, None, "rect")


def header(title: str, subtitle: str, idx: int) -> str:
    return (
        shape(10, 0, 0, SLIDE_W_IN, SLIDE_H_IN, C["bg"], None, "rect")
        + shape(11, 0.0, 0, 0.13, SLIDE_H_IN, C["blue"], None, "rect")
        + text(12, 0.55, 0.28, 9.8, 0.4, title, 25, C["ink"], True)
        + text(13, 0.57, 0.78, 10.8, 0.28, subtitle, 12, C["muted"])
        + text(14, 12.35, 0.35, 0.45, 0.28, f"{idx:02d}", 11, C["muted"], True, "r")
        + line_shape(15, 0.55, 1.13, 1.0, 0.045, C["green"])
    )


def card(sid: int, x: float, y: float, w: float, h: float, title: str, body: str, accent: str, bg: str = "FFFFFF", icon_rid: str | None = None, icon_name: str = "") -> str:
    out = shape(sid, x, y, w, h, bg, "E1E8F0", "roundRect", True)
    out += shape(sid + 1, x, y, 0.08, h, accent, None, "rect")
    if icon_rid:
        out += shape(sid + 2, x + 0.26, y + 0.24, 0.52, 0.52, accent, None, "ellipse")
        out += icon(sid + 3, icon_rid, icon_name, x + 0.36, y + 0.34, 0.32)
        tx = x + 0.92
        tw = w - 1.15
    else:
        tx = x + 0.28
        tw = w - 0.5
    out += text(sid + 4, tx, y + 0.22, tw, 0.32, title, 15, C["ink"], True)
    out += text(sid + 5, tx, y + 0.65, tw, h - 0.75, body, 12, C["muted"])
    return out


def chip(sid: int, x: float, y: float, label: str, color: str) -> str:
    return shape(sid, x, y, 1.45, 0.33, color, None, "roundRect") + text(sid + 1, x + 0.08, y + 0.055, 1.29, 0.16, label, 9, "FFFFFF", True, "ctr")


def bullet_list(sid: int, x: float, y: float, items: list[str], color: str = "16A34A") -> tuple[str, int]:
    out = ""
    cur = sid
    yy = y
    for item in items:
        out += shape(cur, x, yy + 0.11, 0.13, 0.13, color, None, "ellipse")
        out += text(cur + 1, x + 0.28, yy, 5.8, 0.36, item, 14, C["text"])
        yy += 0.48
        cur += 2
    return out, cur


def rels_xml(rels: list[tuple[str, str, str]]) -> str:
    body = "".join(f'<Relationship Id="{rid}" Type="{typ}" Target="{xesc(target)}"/>' for rid, typ, target in rels)
    return f'<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">{body}</Relationships>'


def slide_xml(content: str) -> str:
    return f"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sld xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
       xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
       xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
  <p:cSld>
    <p:bg><p:bgPr>{fill(C["bg"])}<a:effectLst/></p:bgPr></p:bg>
    <p:spTree>
      <p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr>
      <p:grpSpPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="0" cy="0"/><a:chOff x="0" y="0"/><a:chExt cx="0" cy="0"/></a:xfrm></p:grpSpPr>
      {content}
    </p:spTree>
  </p:cSld>
  <p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr>
</p:sld>"""


def make_icons():
    ICON_DIR.mkdir(parents=True, exist_ok=True)
    icons = {
        "scan": "▣",
        "shield": "♢",
        "review": "★",
        "user": "●",
        "admin": "A",
        "leaf": "L",
        "factory": "F",
        "truck": "T",
        "retail": "R",
        "server": "S",
        "mobile": "M",
        "chain": "C",
        "db": "D",
        "web": "W",
        "kafka": "K",
        "warning": "!",
    }
    # Draw simple white pictograms on transparent background. Letters are fallback-safe and small.
    try:
        font_big = ImageFont.truetype("arial.ttf", 118)
        font_med = ImageFont.truetype("arial.ttf", 90)
    except Exception:
        font_big = ImageFont.load_default()
        font_med = ImageFont.load_default()
    for name, glyph in icons.items():
        img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        # custom line icons
        if name == "scan":
            for x, y, sx, sy in [(42, 42, 54, 54), (160, 42, 54, 54), (42, 160, 54, 54)]:
                d.rectangle([x, y, x + sx, y + sy], outline="white", width=12)
                d.rectangle([x + 22, y + 22, x + sx - 22, y + sy - 22], fill="white")
            d.line([142, 168, 214, 168, 214, 214], fill="white", width=12)
        elif name == "shield":
            d.polygon([(128, 28), (210, 64), (196, 168), (128, 226), (60, 168), (46, 64)], outline="white", width=12)
            d.line([86, 130, 118, 162, 176, 94], fill="white", width=14)
        elif name == "review":
            pts = []
            for i in range(10):
                ang = -math.pi / 2 + i * math.pi / 5
                r = 92 if i % 2 == 0 else 40
                pts.append((128 + r * math.cos(ang), 128 + r * math.sin(ang)))
            d.polygon(pts, fill="white")
        elif name == "user":
            d.ellipse([82, 42, 174, 134], outline="white", width=14)
            d.arc([48, 124, 208, 260], 205, 335, fill="white", width=16)
        elif name == "truck":
            d.rectangle([38, 86, 150, 162], outline="white", width=12)
            d.polygon([(150, 110), (198, 110), (224, 140), (224, 162), (150, 162)], outline="white", width=12)
            d.ellipse([64, 150, 104, 190], outline="white", width=10)
            d.ellipse([174, 150, 214, 190], outline="white", width=10)
        elif name == "db":
            d.ellipse([56, 42, 200, 92], outline="white", width=12)
            d.line([56, 67, 56, 178], fill="white", width=12)
            d.line([200, 67, 200, 178], fill="white", width=12)
            d.ellipse([56, 153, 200, 203], outline="white", width=12)
            d.arc([56, 96, 200, 146], 0, 180, fill="white", width=8)
        elif name == "mobile":
            d.rounded_rectangle([72, 30, 184, 226], radius=18, outline="white", width=12)
            d.ellipse([118, 194, 138, 214], fill="white")
        elif name == "chain":
            d.rounded_rectangle([42, 92, 128, 164], radius=30, outline="white", width=12)
            d.rounded_rectangle([128, 92, 214, 164], radius=30, outline="white", width=12)
            d.line([104, 128, 152, 128], fill="white", width=12)
        elif name == "web":
            d.rectangle([40, 54, 216, 190], outline="white", width=12)
            d.line([40, 88, 216, 88], fill="white", width=10)
            d.ellipse([58, 68, 72, 82], fill="white")
            d.ellipse([82, 68, 96, 82], fill="white")
        elif name == "server":
            d.rounded_rectangle([48, 48, 208, 96], radius=10, outline="white", width=10)
            d.rounded_rectangle([48, 112, 208, 160], radius=10, outline="white", width=10)
            d.rounded_rectangle([48, 176, 208, 224], radius=10, outline="white", width=10)
            d.ellipse([174, 64, 190, 80], fill="white")
            d.ellipse([174, 128, 190, 144], fill="white")
            d.ellipse([174, 192, 190, 208], fill="white")
        elif name == "leaf":
            d.ellipse([62, 42, 196, 178], outline="white", width=12)
            d.line([78, 178, 184, 72], fill="white", width=12)
        elif name == "factory":
            d.rectangle([48, 118, 212, 208], outline="white", width=12)
            d.polygon([(48, 118), (90, 86), (90, 118), (134, 86), (134, 118), (178, 86), (178, 118)], outline="white", width=12)
            d.rectangle([66, 62, 94, 118], outline="white", width=10)
        elif name == "warning":
            d.polygon([(128, 34), (224, 204), (32, 204)], outline="white", width=14)
            d.line([128, 92, 128, 150], fill="white", width=14)
            d.ellipse([120, 170, 136, 186], fill="white")
        else:
            bbox = d.textbbox((0, 0), glyph, font=font_big)
            d.text(((256 - (bbox[2] - bbox[0])) / 2, (256 - (bbox[3] - bbox[1])) / 2 - 8), glyph, font=font_big, fill="white")
        img.save(ICON_DIR / f"{name}.png")


slides: list[dict] = []


def add_slide(content: str, images: list[tuple[str, Path]] | None = None):
    slides.append({"content": content, "images": images or []})


def rid_icon(name: str, n: int) -> tuple[str, Path]:
    return (f"rId{n}", ICON_DIR / f"{name}.png")


def img_if(fn: str) -> Path | None:
    p = ASSETS / fn
    return p if p.exists() else None


def build_slides():
    # 1
    imgs = []
    content = shape(10, 0, 0, SLIDE_W_IN, SLIDE_H_IN, C["bg"], None, "rect")
    content += shape(11, 0, 0, SLIDE_W_IN, 0.16, C["blue"], None, "rect")
    content += shape(12, 0.6, 0.66, 6.95, 5.85, C["paper"], "E1E8F0", "roundRect", True)
    content += text(13, 1.02, 1.05, 6.1, 1.65, "Xây dựng hệ thống truy xuất nguồn gốc hàng hóa\nsử dụng công nghệ blockchain và microservice", 28, C["ink"], True)
    content += text(14, 1.04, 2.98, 5.7, 0.34, "Đồ án tốt nghiệp ngành Công nghệ thông tin", 15, C["blue"], True)
    content += line_shape(15, 1.04, 3.48, 1.35, 0.055, C["green"])
    content += text(16, 1.04, 4.0, 5.9, 0.62, "Nguyễn Đăng Nam  |  CT060226  |  CT6B", 16, C["text"], True)
    content += text(17, 1.04, 4.72, 5.7, 0.7, "GVHD: TS. Trần Anh Tú\nThS. Nguyễn Văn Quyết", 14, C["muted"])
    phone = img_if("image54.png") or img_if("image49.png")
    if phone:
        content += shape(20, 8.25, 0.72, 2.65, 5.9, C["dark"], None, "roundRect", True)
        content += pic(21, "rId1", "Mobile demo", 8.43, 0.95, 2.28, 5.35)
        imgs.append(("rId1", phone))
    qr = img_if("image37.png")
    if qr:
        content += shape(22, 10.98, 3.9, 1.65, 1.2, C["paper"], "E1E8F0", "roundRect", True)
        content += pic(23, "rId2", "QR", 11.13, 4.07, 1.35, 0.75)
        imgs.append(("rId2", qr))
    content += chip(30, 8.15, 6.62, "Blockchain", C["blue"]) + chip(32, 9.78, 6.62, "Microservice", C["green"]) + chip(34, 11.4, 6.62, "QR/Serial", C["orange"])
    add_slide(content, imgs)

    # 2
    imgs = [rid_icon("warning", 1), rid_icon("scan", 2), rid_icon("shield", 3)]
    content = header("Bài toán đặt ra", "Tại điểm mua, người dùng cần thông tin đáng tin và dễ kiểm chứng", 2)
    content += card(20, 0.75, 1.55, 3.8, 2.1, "Khó kiểm chứng", "Người mua khó biết sản phẩm thật sự đến từ đâu và đã đi qua những khâu nào.", C["blue"], C["paper"], "rId1", "warning")
    content += card(30, 4.82, 1.55, 3.8, 2.1, "Mã có thể bị sao chép", "Thông tin in trên bao bì hoặc mã truy xuất thông thường có thể bị dùng lại.", C["orange"], C["paper"], "rId2", "scan")
    content += card(40, 8.88, 1.55, 3.8, 2.1, "Dữ liệu cần kiểm chứng", "Nếu dữ liệu trong hệ thống bị sửa, cần có cách phát hiện sai lệch.", C["green"], C["paper"], "rId3", "shield")
    content += shape(50, 1.1, 4.65, 11.1, 1.18, C["blue_soft"], "C9DDFE", "roundRect")
    content += text(51, 1.45, 4.95, 10.4, 0.5, "Hướng giải quyết: xây dựng hệ thống truy xuất nguồn gốc có mobile scan, dữ liệu chuỗi cung ứng và blockchain lưu hash để xác minh.", 19, C["ink"], True, "ctr", "mid")
    add_slide(content, imgs)

    # 3
    imgs = [rid_icon("mobile", 1), rid_icon("review", 2), rid_icon("shield", 3)]
    content = header("Mục tiêu và ý nghĩa thực tiễn", "Slide này nên nói như đang đặt mình vào vị trí người mua hàng", 3)
    content += card(20, 0.78, 1.55, 3.82, 2.35, "Xem thông tin ngay tại siêu thị", "Quét QR hoặc nhập serial để xem nguồn gốc, lịch sử vận chuyển và chi tiết sản phẩm.", C["blue"], C["paper"], "rId1", "mobile")
    content += card(30, 4.82, 1.55, 3.82, 2.35, "Tham khảo đánh giá", "Người mua sau có thể xem đánh giá từ người đã mua và sử dụng sản phẩm trước đó.", C["green"], C["paper"], "rId2", "review")
    content += card(40, 8.88, 1.55, 3.82, 2.35, "Hỗ trợ chống giả", "Mỗi sản phẩm gắn với một serial tờ tiền đã đăng ký; serial chỉ dùng một lần để phát hiện dùng lại hoặc không khớp.", C["orange"], C["paper"], "rId3", "shield")
    content += text(50, 1.0, 4.65, 11.4, 0.92, "Mục tiêu kỹ thuật: quản lý chuỗi cung ứng nhiều vai trò, truy xuất ngược đến nguyên liệu và xác minh tính toàn vẹn dữ liệu bằng blockchain.", 21, C["ink"], True, "ctr", "mid")
    add_slide(content, imgs)

    # 4
    role_icons = ["admin", "leaf", "factory", "retail", "truck", "user"]
    imgs = [rid_icon(name, i + 1) for i, name in enumerate(role_icons)]
    content = header("Phạm vi và tác nhân", "Mỗi vai trò tương ứng một lát cắt nghiệp vụ trong hệ thống", 4)
    roles = [
        ("Admin", "Duyệt vai trò,\nquản lý người dùng", C["blue"]),
        ("Supplier", "Tạo và quản lý\nlô nguyên liệu", C["green"]),
        ("Manufacturer", "Sản xuất, đóng gói,\nđăng ký serial", C["orange"]),
        ("Retailer", "Đặt hàng\nthành phẩm", C["blue"]),
        ("Transporter", "Xác nhận lấy hàng\nvà giao hàng", C["green"]),
        ("User", "Quét truy xuất,\nxem/đánh giá SP", C["orange"]),
    ]
    for i, (name, body, accent) in enumerate(roles):
        x = 0.78 + (i % 3) * 4.15
        y = 1.55 + (i // 3) * 2.15
        content += card(20 + i * 10, x, y, 3.72, 1.65, name, body, accent, C["paper"], f"rId{i+1}", role_icons[i])
    add_slide(content, imgs)

    # 5 architecture custom
    imgs = [rid_icon("web", 1), rid_icon("mobile", 2), rid_icon("server", 3), rid_icon("db", 4), rid_icon("kafka", 5), rid_icon("chain", 6)]
    content = header("Kiến trúc tổng thể", "Thiết kế theo hướng microservices, có gateway và xử lý bất đồng bộ", 5)
    content += card(20, 0.8, 1.65, 2.6, 1.2, "React Web", "Dashboard nghiệp vụ", C["blue"], C["paper"], "rId1", "web")
    content += card(30, 0.8, 3.35, 2.6, 1.2, "Flutter Mobile", "Scan, retailer, transporter", C["green"], C["paper"], "rId2", "mobile")
    content += line_shape(40, 3.55, 2.28, 0.75, 0.06, C["line"]) + line_shape(41, 3.55, 3.98, 0.75, 0.06, C["line"])
    content += card(50, 4.35, 2.42, 2.3, 1.18, "API Gateway", "Điểm vào chung", C["orange"], C["orange_soft"], "rId3", "server")
    content += line_shape(60, 6.78, 3.0, 0.65, 0.06, C["line"])
    services = ["Identity", "Catalog", "Core", "Trade", "Blockchain"]
    for i, svc in enumerate(services):
        x = 7.55 + (i % 3) * 1.58
        y = 1.55 + (i // 3) * 1.52
        content += shape(70 + i * 3, x, y, 1.35, 0.76, C["paper"], "D7E2EF", "roundRect", True)
        content += text(71 + i * 3, x + 0.05, y + 0.26, 1.25, 0.2, svc, 11, C["ink"], True, "ctr")
    content += card(90, 7.55, 4.65, 1.85, 1.25, "PostgreSQL", "Lưu nghiệp vụ", C["blue"], C["blue_soft"], "rId4", "db")
    content += card(100, 9.72, 4.65, 1.85, 1.25, "Kafka", "Event async", C["green"], C["green_soft"], "rId5", "kafka")
    content += card(110, 11.88, 4.65, 1.1, 1.25, "Chain", "Hash", C["orange"], C["orange_soft"], "rId6", "chain")
    add_slide(content, imgs)

    # 6 tech
    imgs = [rid_icon("server", 1), rid_icon("web", 2), rid_icon("mobile", 3), rid_icon("chain", 4)]
    content = header("Công nghệ sử dụng", "Chia theo các lớp triển khai chính", 6)
    stacks = [
        ("Backend", "Spring Boot\nSpring Security\nEureka, Feign", C["blue"], C["blue_soft"], "rId1", "server"),
        ("Frontend Web", "React, Vite\nAnt Design\nReact Query", C["green"], C["green_soft"], "rId2", "web"),
        ("Mobile", "Flutter, Bloc\nDio\nQR/OCR Scanner", C["orange"], C["orange_soft"], "rId3", "mobile"),
        ("Blockchain & Hạ tầng", "Solidity, Web3j\nPostgreSQL, Kafka\nWebSocket, Ganache", C["blue"], C["blue_soft"], "rId4", "chain"),
    ]
    for i, s in enumerate(stacks):
        content += card(20 + i * 10, 0.78 + i * 3.05, 1.75, 2.72, 3.35, *s)
    content += text(75, 1.05, 5.9, 11.2, 0.48, "Lựa chọn công nghệ phục vụ ba yêu cầu: chia tách nghiệp vụ, truy xuất trên thiết bị di động và xác minh dữ liệu bằng blockchain.", 17, C["ink"], True, "ctr")
    add_slide(content, imgs)

    # 7 business flow
    content = header("Luồng nghiệp vụ chính", "Tóm tắt đường đi của hàng hóa trong hệ thống", 7)
    steps = [
        ("01", "Supplier\ntạo nguyên liệu", C["green"]),
        ("02", "Manufacturer\nmua nguyên liệu", C["blue"]),
        ("03", "Sản xuất\npallet", C["orange"]),
        ("04", "Đóng gói\ncarton/unit", C["blue"]),
        ("05", "Retailer\nđặt hàng", C["green"]),
        ("06", "Transporter\ngiao hàng", C["orange"]),
        ("07", "User\nquét truy xuất", C["blue"]),
    ]
    for i, (num, label, col) in enumerate(steps):
        x = 0.58 + i * 1.78
        content += shape(20 + i * 5, x, 2.0, 0.86, 0.86, col, None, "ellipse", True)
        content += text(21 + i * 5, x, 2.28, 0.86, 0.2, num, 12, "FFFFFF", True, "ctr")
        content += text(22 + i * 5, x - 0.38, 3.15, 1.58, 0.65, label, 13, C["ink"], True, "ctr")
        if i < len(steps) - 1:
            content += line_shape(23 + i * 5, x + 0.95, 2.42, 0.68, 0.055, C["line"])
    content += shape(80, 0.92, 5.05, 11.55, 1.0, C["paper"], "E1E8F0", "roundRect", True)
    content += text(81, 1.25, 5.34, 10.9, 0.42, "Điểm đáng chú ý: hệ thống quản lý cả dữ liệu truy xuất và trạng thái giao dịch/vận chuyển giữa nhiều vai trò.", 18, C["text"], True, "ctr")
    add_slide(content)

    # 8 trace
    imgs = [rid_icon("db", 1), rid_icon("chain", 2), rid_icon("scan", 3)]
    content = header("Mô hình truy xuất nguồn gốc", "Truy ngược từ một sản phẩm lẻ về nguyên liệu ban đầu", 8)
    nodes = [
        ("Raw Batch", "Lô nguyên liệu", C["green"]),
        ("Pallet", "Lô sản xuất", C["green"]),
        ("Carton", "Thùng hàng", C["blue"]),
        ("Product Unit", "Sản phẩm lẻ", C["blue"]),
        ("QR / Serial", "Mã truy xuất", C["orange"]),
        ("User Scan", "Người dùng", C["orange"]),
    ]
    for i, (title, sub, col) in enumerate(nodes):
        x = 0.6 + i * 2.08
        bg = C["green_soft"] if col == C["green"] else C["blue_soft"] if col == C["blue"] else C["orange_soft"]
        content += shape(20 + i * 5, x, 2.0, 1.5, 0.96, bg, "DCE5EF", "roundRect", True)
        content += text(21 + i * 5, x + 0.08, 2.22, 1.34, 0.2, title, 12, C["ink"], True, "ctr")
        content += text(22 + i * 5, x + 0.08, 2.55, 1.34, 0.18, sub, 9, C["muted"], False, "ctr")
        if i < 5:
            content += line_shape(23 + i * 5, x + 1.58, 2.45, 0.38, 0.055, col)
    content += card(60, 0.95, 4.35, 3.7, 1.35, "Dữ liệu nghiệp vụ", "Product, carton, pallet, raw batch và transfer record tạo thành timeline.", C["blue"], C["paper"], "rId1", "db")
    content += card(70, 4.95, 4.35, 3.7, 1.35, "Blockchain", "Hash dùng để kiểm chứng tính toàn vẹn của dữ liệu.", C["green"], C["paper"], "rId2", "chain")
    content += card(80, 8.95, 4.35, 3.7, 1.35, "Mobile scan", "Người dùng xem timeline và kết quả xác minh.", C["orange"], C["paper"], "rId3", "scan")
    add_slide(content, imgs)

    # 9 blockchain
    imgs = [rid_icon("db", 1), rid_icon("chain", 2), rid_icon("shield", 3)]
    content = header("Cơ chế blockchain và xác minh", "Không đưa toàn bộ dữ liệu lên blockchain, chỉ neo hash quan trọng", 9)
    flow = [
        ("Dữ liệu nghiệp vụ", "Lưu trong PostgreSQL", "rId1", C["blue"]),
        ("Tính hash", "Chuẩn hóa và băm dữ liệu", "rId3", C["green"]),
        ("Ghi blockchain", "Lưu hash + event audit", "rId2", C["orange"]),
        ("Verify", "Tính lại và so sánh", "rId3", C["blue"]),
    ]
    for i, (title, body, rid, col) in enumerate(flow):
        x = 0.82 + i * 3.08
        content += card(20 + i * 10, x, 1.85, 2.55, 1.5, title, body, col, C["paper"], rid, title)
        if i < 3:
            content += line_shape(25 + i * 10, x + 2.66, 2.58, 0.44, 0.055, C["line"])
    content += card(70, 1.08, 4.35, 5.35, 1.36, "Khi dữ liệu còn nguyên vẹn", "Hash tính lại từ DB khớp với hash đã ghi trên blockchain.", C["green"], C["green_soft"], "rId3", "shield")
    content += card(80, 6.88, 4.35, 5.35, 1.36, "Khi dữ liệu bị sửa", "Hash không khớp, hệ thống cảnh báo dữ liệu truy xuất không còn toàn vẹn.", C["red"], C["red_soft"], "rId3", "shield")
    add_slide(content, imgs)

    # 10 anti + review
    imgs = [rid_icon("shield", 1), rid_icon("review", 2)]
    demo1 = img_if("image58.png")
    demo2 = img_if("image53.png") or img_if("image49.png")
    content = header("Chống giả và đánh giá sản phẩm", "Hai chức năng giúp người dùng có thêm niềm tin khi mua hàng", 10)
    content += card(20, 0.78, 1.55, 5.55, 2.05, "Chống giả bằng serial tờ tiền", "Mỗi sản phẩm gắn với một serial tờ tiền đã đăng ký; serial chỉ được dùng một lần.\nNếu serial bị dùng lại hoặc không khớp sản phẩm, hệ thống phát hiện bất thường.", C["orange"], C["orange_soft"], "rId1", "shield")
    content += card(30, 0.78, 3.95, 5.55, 1.65, "Đánh giá sản phẩm", "Người dùng đã mua có thể đánh giá, người mua sau có thêm thông tin tham khảo thực tế.", C["green"], C["green_soft"], "rId2", "review")
    local_imgs = imgs[:]
    if demo1:
        content += shape(45, 7.2, 1.42, 1.82, 4.15, C["dark"], None, "roundRect", True)
        content += pic(46, "rId3", "Review mobile", 7.34, 1.65, 1.54, 3.68)
        local_imgs.append(("rId3", demo1))
    if demo2:
        content += shape(47, 9.45, 1.42, 1.82, 4.15, C["dark"], None, "roundRect", True)
        content += pic(48, "rId4", "Trace mobile", 9.59, 1.65, 1.54, 3.68)
        local_imgs.append(("rId4", demo2))
    add_slide(content, local_imgs)

    # 11 results/demo
    content = header("Kết quả đạt được", "Các chức năng chính đã hoàn thiện để demo bảo vệ", 11)
    img_list = []
    pics = [("image41.png", 0.72, 1.45, 3.7, 1.24), ("image40.png", 4.65, 1.45, 3.7, 1.75), ("image62.png", 8.72, 1.28, 1.32, 2.95), ("image54.png", 10.25, 1.28, 1.32, 2.95), ("image58.png", 11.78, 1.28, 1.32, 2.95)]
    ridn = 1
    for fn, x, y, w, h in pics:
        p = img_if(fn)
        if p:
            if h > 2.5:
                content += shape(30 + ridn, x - 0.08, y - 0.1, w + 0.16, h + 0.2, C["dark"], None, "roundRect", True)
            else:
                content += shape(30 + ridn, x - 0.05, y - 0.05, w + 0.1, h + 0.1, C["paper"], "E1E8F0", "roundRect", True)
            content += pic(50 + ridn, f"rId{ridn}", fn, x, y, w, h)
            img_list.append((f"rId{ridn}", p))
            ridn += 1
    checks = [
        "Backend microservices: Identity, Catalog, Core, Trade Logistics, Blockchain.",
        "Web nghiệp vụ cho admin, supplier, manufacturer.",
        "Mobile: scan truy xuất, đánh giá, retailer order, transporter delivery.",
        "Blockchain: ghi hash, verify dữ liệu, thống kê gas; Kafka/WebSocket cho xử lý bất đồng bộ.",
    ]
    bl, _ = bullet_list(80, 1.0, 4.75, checks, C["green"])
    content += bl
    add_slide(content, img_list)

    # 12
    imgs = [rid_icon("shield", 1), rid_icon("warning", 2), rid_icon("chain", 3)]
    content = header("Đánh giá và kết luận", "Kết thúc ngắn, để dành thời gian cho phần hỏi đáp", 12)
    content += card(20, 0.85, 1.55, 3.75, 2.0, "Ưu điểm", "Có tính thực tiễn, nhiều vai trò, luồng nghiệp vụ tương đối đầy đủ.\nBlockchain dùng hợp lý để xác minh dữ liệu.", C["green"], C["green_soft"], "rId1", "shield")
    content += card(30, 4.85, 1.55, 3.75, 2.0, "Hạn chế", "Môi trường còn mang tính demo local.\nKiểm thử tự động và bảo mật cấu hình cần hoàn thiện thêm.", C["orange"], C["orange_soft"], "rId2", "warning")
    content += card(40, 8.85, 1.55, 3.75, 2.0, "Hướng phát triển", "Triển khai cloud, quản lý secret chuẩn hơn, mở rộng dashboard phân tích và test tích hợp.", C["blue"], C["blue_soft"], "rId3", "chain")
    content += shape(60, 1.0, 4.62, 11.35, 0.95, C["paper"], "E1E8F0", "roundRect", True)
    content += text(61, 1.35, 4.9, 10.7, 0.35, "Hệ thống đáp ứng mục tiêu truy xuất nguồn gốc, minh bạch dữ liệu chuỗi cung ứng và hỗ trợ chống giả sản phẩm.", 20, C["ink"], True, "ctr")
    content += text(62, 1.0, 6.25, 11.35, 0.42, "Em xin chân thành cảm ơn!", 22, C["blue"], True, "ctr")
    add_slide(content, imgs)


def build_package():
    with zipfile.ZipFile(OUT, "w", zipfile.ZIP_DEFLATED) as z:
        overrides = [
            '<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>',
            '<Default Extension="xml" ContentType="application/xml"/>',
            '<Default Extension="png" ContentType="image/png"/>',
            '<Default Extension="jpg" ContentType="image/jpeg"/>',
            '<Default Extension="jpeg" ContentType="image/jpeg"/>',
            '<Override PartName="/ppt/presentation.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml"/>',
            '<Override PartName="/ppt/slideMasters/slideMaster1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slideMaster+xml"/>',
            '<Override PartName="/ppt/slideLayouts/slideLayout1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slideLayout+xml"/>',
            '<Override PartName="/ppt/theme/theme1.xml" ContentType="application/vnd.openxmlformats-officedocument.theme+xml"/>',
            '<Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>',
            '<Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>',
        ]
        for i in range(1, len(slides) + 1):
            overrides.append(f'<Override PartName="/ppt/slides/slide{i}.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slide+xml"/>')
        z.writestr("[Content_Types].xml", '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">' + "".join(overrides) + "</Types>")
        z.writestr("_rels/.rels", rels_xml([
            ("rId1", "http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument", "ppt/presentation.xml"),
            ("rId2", "http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties", "docProps/core.xml"),
            ("rId3", "http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties", "docProps/app.xml"),
        ]))
        z.writestr("docProps/core.xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/"><dc:title>DATN - Truy xuất nguồn gốc blockchain</dc:title><dc:creator>Nguyễn Đăng Nam</dc:creator><cp:lastModifiedBy>Codex</cp:lastModifiedBy></cp:coreProperties>""")
        z.writestr("docProps/app.xml", f"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties" xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes"><Application>Microsoft PowerPoint</Application><Slides>{len(slides)}</Slides></Properties>""")
        sld_ids = "".join(f'<p:sldId id="{255+i}" r:id="rId{i+1}"/>' for i in range(1, len(slides) + 1))
        z.writestr("ppt/presentation.xml", f"""<?xml version="1.0" encoding="UTF-8" standalone="yes"?><p:presentation xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main"><p:sldMasterIdLst><p:sldMasterId id="2147483648" r:id="rId1"/></p:sldMasterIdLst><p:sldIdLst>{sld_ids}</p:sldIdLst><p:sldSz cx="{SLIDE_W}" cy="{SLIDE_H}" type="wide"/><p:notesSz cx="6858000" cy="9144000"/><p:defaultTextStyle/></p:presentation>""")
        pres_rels = [("rId1", "http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster", "slideMasters/slideMaster1.xml")]
        for i in range(1, len(slides) + 1):
            pres_rels.append((f"rId{i+1}", "http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide", f"slides/slide{i}.xml"))
        z.writestr("ppt/_rels/presentation.xml.rels", rels_xml(pres_rels))
        z.writestr("ppt/slideMasters/slideMaster1.xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><p:sldMaster xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main"><p:cSld><p:spTree><p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr><p:grpSpPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="0" cy="0"/><a:chOff x="0" y="0"/><a:chExt cx="0" cy="0"/></a:xfrm></p:grpSpPr></p:spTree></p:cSld><p:clrMap bg1="lt1" tx1="dk1" bg2="lt2" tx2="dk2" accent1="accent1" accent2="accent2" accent3="accent3" accent4="accent4" accent5="accent5" accent6="accent6" hlink="hlink" folHlink="folHlink"/><p:sldLayoutIdLst><p:sldLayoutId id="2147483649" r:id="rId2"/></p:sldLayoutIdLst><p:txStyles><p:titleStyle/><p:bodyStyle/><p:otherStyle/></p:txStyles></p:sldMaster>""")
        z.writestr("ppt/slideMasters/_rels/slideMaster1.xml.rels", rels_xml([
            ("rId1", "http://schemas.openxmlformats.org/officeDocument/2006/relationships/theme", "../theme/theme1.xml"),
            ("rId2", "http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout", "../slideLayouts/slideLayout1.xml"),
        ]))
        z.writestr("ppt/slideLayouts/slideLayout1.xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><p:sldLayout xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main" type="blank" preserve="1"><p:cSld name="Blank"><p:spTree><p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr><p:grpSpPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="0" cy="0"/><a:chOff x="0" y="0"/><a:chExt cx="0" cy="0"/></a:xfrm></p:grpSpPr></p:spTree></p:cSld><p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr></p:sldLayout>""")
        z.writestr("ppt/slideLayouts/_rels/slideLayout1.xml.rels", rels_xml([
            ("rId1", "http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster", "../slideMasters/slideMaster1.xml")
        ]))
        z.writestr("ppt/theme/theme1.xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><a:theme xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" name="DATN Clean"><a:themeElements><a:clrScheme name="DATN"><a:dk1><a:srgbClr val="0B1220"/></a:dk1><a:lt1><a:srgbClr val="FFFFFF"/></a:lt1><a:dk2><a:srgbClr val="1F2937"/></a:dk2><a:lt2><a:srgbClr val="F7FAFC"/></a:lt2><a:accent1><a:srgbClr val="2563EB"/></a:accent1><a:accent2><a:srgbClr val="16A34A"/></a:accent2><a:accent3><a:srgbClr val="F97316"/></a:accent3><a:accent4><a:srgbClr val="DC2626"/></a:accent4><a:accent5><a:srgbClr val="64748B"/></a:accent5><a:accent6><a:srgbClr val="0F172A"/></a:accent6><a:hlink><a:srgbClr val="2563EB"/></a:hlink><a:folHlink><a:srgbClr val="1D4ED8"/></a:folHlink></a:clrScheme><a:fontScheme name="Aptos"><a:majorFont><a:latin typeface="Aptos Display"/></a:majorFont><a:minorFont><a:latin typeface="Aptos"/></a:minorFont></a:fontScheme><a:fmtScheme name="Office"><a:fillStyleLst><a:solidFill><a:schemeClr val="phClr"/></a:solidFill></a:fillStyleLst><a:lnStyleLst><a:ln w="9525"><a:solidFill><a:schemeClr val="phClr"/></a:solidFill></a:ln></a:lnStyleLst><a:effectStyleLst><a:effectStyle><a:effectLst/></a:effectStyle></a:effectStyleLst><a:bgFillStyleLst><a:solidFill><a:schemeClr val="phClr"/></a:solidFill></a:bgFillStyleLst></a:fmtScheme></a:themeElements></a:theme>""")

        media_counter = 1
        for i, s in enumerate(slides, start=1):
            z.writestr(f"ppt/slides/slide{i}.xml", slide_xml(s["content"]))
            rels = [("rIdLayout", "http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout", "../slideLayouts/slideLayout1.xml")]
            for rid, path in s["images"]:
                ext = path.suffix.lower().lstrip(".")
                target = f"../media/media{media_counter}.{ext}"
                z.write(path, f"ppt/media/media{media_counter}.{ext}")
                rels.append((rid, "http://schemas.openxmlformats.org/officeDocument/2006/relationships/image", target))
                media_counter += 1
            z.writestr(f"ppt/slides/_rels/slide{i}.xml.rels", rels_xml(rels))


if __name__ == "__main__":
    make_icons()
    build_slides()
    build_package()
    print(OUT)
