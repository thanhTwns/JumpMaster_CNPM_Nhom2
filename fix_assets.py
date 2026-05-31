from PIL import Image
import os, io

folder = r"D:\CNPM_Game\assets\ui-timeAttack"

# Danh sách file cần convert
targets = [f"vampire{i}" for i in range(1, 13)] + ["portal"]

for name in targets:
    # Tìm file nguồn (.webp hoặc .png bị đổi tên sai)
    src = None
    for ext in [".webp", ".png"]:
        candidate = os.path.join(folder, name + ext)
        if os.path.exists(candidate):
            src = candidate
            break
    
    if src is None:
        print(f"KHÔNG TÌM THẤY: {name}")
        continue
    
    try:
        with open(src, 'rb') as f:
            data = f.read()
        
        img = Image.open(io.BytesIO(data))
        img = img.convert("RGBA")
        
        out = os.path.join(folder, name + ".png")
        img.save(out, "PNG")
        print(f"OK: {name} -> {name}.png | kích thước={img.size}")
        
    except Exception as e:
        print(f"LỖI: {name} -> {e}")

print("\nHoàn thành!")