-- =============================================================================
-- HỆ THỐNG QUẢN LÝ CHUNG CƯ (APARTMENT MANAGEMENT SYSTEM)
-- DATABASE SCHEMA DDL SCRIPT (POSTGRESQL / ANSI SQL COMPATIBLE)
-- Học phần: IT3930 - Project II | GVHD: PGS. TS Trần Đình Khang
-- Sinh viên: Đường Khôi Nguyên (20235180) - Vũ Đình Khôi (20235124)
-- Nguồn dữ liệu: Tài liệu Phân tích & Thiết kế Hệ thống Quản lý Chung cư
-- =============================================================================

-- =============================================================================
-- PHẦN 1: NHÓM BẢNG QUẢN TRỊ NGƯỜI DÙNG & TÁC NHÂN (MÔ HÌNH KẾ THỪA TPT)
-- =============================================================================

-- 1. Bảng NguoiDungHeThong: Định danh gốc và tài khoản đăng nhập
CREATE TABLE NguoiDungHeThong (
    maNguoiDung VARCHAR(30) PRIMARY KEY,
    hoTen VARCHAR(100) NOT NULL,
    soDienThoai VARCHAR(15) NOT NULL UNIQUE,
    email VARCHAR(150) NOT NULL UNIQUE,
    matKhauHash VARCHAR(255) NOT NULL,
    trangThaiTaiKhoan VARCHAR(30) DEFAULT 'Hoạt động' NOT NULL,
    CONSTRAINT chk_trangThaiTaiKhoan CHECK (trangThaiTaiKhoan IN ('Hoạt động', 'Tạm khóa', 'Ngừng hoạt động'))
);

-- 2. Bảng NhanVienBQL: Nhân viên ban quản lý vận hành
CREATE TABLE NhanVienBQL (
    maNV VARCHAR(30) PRIMARY KEY,
    maNguoiDung VARCHAR(30) NOT NULL UNIQUE,
    chucVu VARCHAR(100) NOT NULL,
    boPhan VARCHAR(100) NOT NULL,
    CONSTRAINT fk_nvbql_nguoidung FOREIGN KEY (maNguoiDung) 
        REFERENCES NguoiDungHeThong(maNguoiDung) ON DELETE CASCADE
);

-- 3. Bảng NhanVienKyThuat: Nhân viên kỹ thuật bảo trì, ghi chỉ số
CREATE TABLE NhanVienKyThuat (
    maNV VARCHAR(30) PRIMARY KEY,
    maNguoiDung VARCHAR(30) NOT NULL UNIQUE,
    chuyenMonKyThuat VARCHAR(150) NOT NULL,
    CONSTRAINT fk_nvkt_nguoidung FOREIGN KEY (maNguoiDung) 
        REFERENCES NguoiDungHeThong(maNguoiDung) ON DELETE CASCADE
);

-- 4. Bảng KeToan: Kế toán tài chính, công nợ
CREATE TABLE KeToan (
    maNV VARCHAR(30) PRIMARY KEY,
    maNguoiDung VARCHAR(30) NOT NULL UNIQUE,
    phamViPhuTrach VARCHAR(150) NOT NULL,
    CONSTRAINT fk_ketoan_nguoidung FOREIGN KEY (maNguoiDung) 
        REFERENCES NguoiDungHeThong(maNguoiDung) ON DELETE CASCADE
);

-- 5. Bảng ThanhVienBanQuanTri: Ban quản trị chung cư
CREATE TABLE ThanhVienBanQuanTri (
    maThanhVien VARCHAR(30) PRIMARY KEY,
    maNguoiDung VARCHAR(30) NOT NULL UNIQUE,
    nhiemKy VARCHAR(50) NOT NULL,
    vaiTro VARCHAR(100) NOT NULL,
    CONSTRAINT fk_bqt_nguoidung FOREIGN KEY (maNguoiDung) 
        REFERENCES NguoiDungHeThong(maNguoiDung) ON DELETE CASCADE
);

-- 6. Bảng CuDan: Thông tin cư dân sinh sống
CREATE TABLE CuDan (
    maCuDan VARCHAR(30) PRIMARY KEY,
    maNguoiDung VARCHAR(30) NOT NULL UNIQUE,
    soCCCD VARCHAR(20) NOT NULL UNIQUE,
    CONSTRAINT fk_cudan_nguoidung FOREIGN KEY (maNguoiDung) 
        REFERENCES NguoiDungHeThong(maNguoiDung) ON DELETE CASCADE
);

-- =============================================================================
-- PHẦN 2: NHÓM BẢNG KHÔNG GIAN VẬT LÝ, CĂN HỘ & CƯ TRÚ
-- =============================================================================

-- 7. Bảng ToaNha: Quản lý danh mục tòa nhà (Block)
CREATE TABLE ToaNha (
    maToaNha VARCHAR(20) PRIMARY KEY,
    tenToaNha VARCHAR(100) NOT NULL UNIQUE,
    soTang INTEGER NOT NULL,
    diaChiKhuVuc VARCHAR(255) NOT NULL,
    CONSTRAINT chk_toanha_soTang CHECK (soTang > 0)
);

-- 8. Bảng Tang: Các tầng trong tòa nhà
CREATE TABLE Tang (
    maTang VARCHAR(20) PRIMARY KEY,
    maToaNha VARCHAR(20) NOT NULL,
    soTang INTEGER NOT NULL,
    CONSTRAINT fk_tang_toanha FOREIGN KEY (maToaNha) 
        REFERENCES ToaNha(maToaNha) ON DELETE RESTRICT,
    CONSTRAINT uq_tang_toanha_sotang UNIQUE (maToaNha, soTang)
);

-- 9. Bảng CanHo: Căn hộ thuộc tầng
CREATE TABLE CanHo (
    maCanHo VARCHAR(20) PRIMARY KEY,
    maTang VARCHAR(20) NOT NULL,
    dienTich NUMERIC(8,2) NOT NULL,
    trangThaiSuDung VARCHAR(40) DEFAULT 'Trống' NOT NULL,
    biTamDungTienIch BOOLEAN DEFAULT FALSE NOT NULL,
    CONSTRAINT fk_canho_tang FOREIGN KEY (maTang) 
        REFERENCES Tang(maTang) ON DELETE RESTRICT,
    CONSTRAINT chk_canho_dienTich CHECK (dienTich > 0),
    CONSTRAINT chk_canho_trangThai CHECK (trangThaiSuDung IN ('Trống', 'Đang sử dụng', 'Đang sửa chữa', 'Đã bàn giao'))
);

-- 10. Bảng KhuVucDungChung: Khu vực công cộng, sảnh, phòng kỹ thuật
CREATE TABLE KhuVucDungChung (
    maKhuVuc VARCHAR(30) PRIMARY KEY,
    maToaNha VARCHAR(20) NOT NULL,
    tenKhuVuc VARCHAR(150) NOT NULL,
    viTri VARCHAR(150) NOT NULL,
    CONSTRAINT fk_khuvuc_toanha FOREIGN KEY (maToaNha) 
        REFERENCES ToaNha(maToaNha) ON DELETE RESTRICT
);

-- 11. Bảng HoSoCuTru: Mối quan hệ cư trú giữa Cư dân và Căn hộ
CREATE TABLE HoSoCuTru (
    maHoSo VARCHAR(30) PRIMARY KEY,
    maCanHo VARCHAR(20) NOT NULL,
    maCuDan VARCHAR(30) NOT NULL,
    maNVBQLXacMinh VARCHAR(30) NOT NULL,
    vaiTroCuTru VARCHAR(40) NOT NULL,
    tinhTrangCuTru VARCHAR(40) NOT NULL,
    ngayBatDau DATE NOT NULL,
    ngayKetThuc DATE NULL,
    trangThaiHoSo VARCHAR(40) DEFAULT 'Hợp lệ' NOT NULL,
    CONSTRAINT fk_hsct_canho FOREIGN KEY (maCanHo) 
        REFERENCES CanHo(maCanHo) ON DELETE RESTRICT,
    CONSTRAINT fk_hsct_cudan FOREIGN KEY (maCuDan) 
        REFERENCES CuDan(maCuDan) ON DELETE RESTRICT,
    CONSTRAINT fk_hsct_nvbql FOREIGN KEY (maNVBQLXacMinh) 
        REFERENCES NhanVienBQL(maNV),
    CONSTRAINT chk_hsct_vaiTro CHECK (vaiTroCuTru IN ('Chủ căn hộ', 'Khách thuê', 'Thành viên gia đình')),
    CONSTRAINT chk_hsct_tinhTrang CHECK (tinhTrangCuTru IN ('Thường trú', 'Tạm trú')),
    CONSTRAINT chk_hsct_ngay CHECK (ngayKetThuc IS NULL OR ngayKetThuc >= ngayBatDau),
    CONSTRAINT chk_hsct_trangThai CHECK (trangThaiHoSo IN ('Chờ xác minh', 'Hợp lệ', 'Hết hiệu lực'))
);

-- 12. Bảng PhuongTien: Phương tiện của cư dân gắn với căn hộ
CREATE TABLE PhuongTien (
    maPhuongTien VARCHAR(30) PRIMARY KEY,
    maCanHo VARCHAR(20) NOT NULL,
    maCuDanSoHuu VARCHAR(30) NOT NULL,
    loaiPhuongTien VARCHAR(40) NOT NULL,
    bienSo VARCHAR(25) NOT NULL UNIQUE,
    nhanHieuMauSac VARCHAR(100) NULL,
    trangThaiDangKy VARCHAR(40) DEFAULT 'Đang hoạt động' NOT NULL,
    CONSTRAINT fk_phuongtien_canho FOREIGN KEY (maCanHo) 
        REFERENCES CanHo(maCanHo) ON DELETE RESTRICT,
    CONSTRAINT fk_phuongtien_cudan FOREIGN KEY (maCuDanSoHuu) 
        REFERENCES CuDan(maCuDan),
    CONSTRAINT chk_phuongtien_loai CHECK (loaiPhuongTien IN ('Ô tô', 'Xe máy', 'Xe máy điện', 'Xe đạp')),
    CONSTRAINT chk_phuongtien_trangThai CHECK (trangThaiDangKy IN ('Đang hoạt động', 'Tạm dừng', 'Đã hủy'))
);

-- =============================================================================
-- PHẦN 3: NHÓM BẢNG ĐIỆN NƯỚC & TÀI CHÍNH - CÔNG NỢ
-- =============================================================================

-- 13. Bảng DongHoTieuThu: Đồng hồ đo điện, nước lắp tại căn hộ hoặc khu vực chung
CREATE TABLE DongHoTieuThu (
    maDongHo VARCHAR(30) PRIMARY KEY,
    maCanHo VARCHAR(20) NULL,
    maKhuVuc VARCHAR(30) NULL,
    loaiDongHo VARCHAR(20) NOT NULL,
    viTriLapDat VARCHAR(150) NOT NULL,
    trangThaiDongHo VARCHAR(40) DEFAULT 'Đang hoạt động' NOT NULL,
    CONSTRAINT fk_dongho_canho FOREIGN KEY (maCanHo) 
        REFERENCES CanHo(maCanHo),
    CONSTRAINT fk_dongho_khuvuc FOREIGN KEY (maKhuVuc) 
        REFERENCES KhuVucDungChung(maKhuVuc),
    CONSTRAINT chk_dongho_loai CHECK (loaiDongHo IN ('Điện', 'Nước')),
    CONSTRAINT chk_dongho_trangThai CHECK (trangThaiDongHo IN ('Đang hoạt động', 'Đã ghi nhận', 'Chưa ghi nhận/Đồng hồ lỗi', 'Ngừng sử dụng')),
    CONSTRAINT chk_dongho_xor CHECK ((maCanHo IS NOT NULL AND maKhuVuc IS NULL) OR (maCanHo IS NULL AND maKhuVuc IS NOT NULL))
);

-- 14. Bảng BangGhiChiSo: Lịch sử ghi chỉ số điện nước theo kỳ
CREATE TABLE BangGhiChiSo (
    maBanGhi VARCHAR(30) PRIMARY KEY,
    maDongHo VARCHAR(30) NOT NULL,
    maNVKyThuat VARCHAR(30) NOT NULL,
    maNVBQLXacMinh VARCHAR(30) NULL,
    kyGhiChiSo VARCHAR(15) NOT NULL,
    chiSoKyTruoc NUMERIC(12,2) NOT NULL,
    chiSoHienTai NUMERIC(12,2) NULL,
    luongTieuThu NUMERIC(12,2) NULL,
    anhMinhChung VARCHAR(255) NULL,
    ghiChuBatThuong TEXT NULL,
    lyDoChinhSua TEXT NULL,
    thoiGianGhi TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    trangThaiGhi VARCHAR(40) DEFAULT 'Đã ghi nhận' NOT NULL,
    CONSTRAINT fk_bgcs_dongho FOREIGN KEY (maDongHo) 
        REFERENCES DongHoTieuThu(maDongHo),
    CONSTRAINT fk_bgcs_nvkt FOREIGN KEY (maNVKyThuat) 
        REFERENCES NhanVienKyThuat(maNV),
    CONSTRAINT fk_bgcs_nvbql FOREIGN KEY (maNVBQLXacMinh) 
        REFERENCES NhanVienBQL(maNV),
    CONSTRAINT uq_bgcs_dongho_ky UNIQUE (maDongHo, kyGhiChiSo),
    CONSTRAINT chk_bgcs_chisokytruoc CHECK (chiSoKyTruoc >= 0),
    CONSTRAINT chk_bgcs_chisohientai CHECK (chiSoHienTai IS NULL OR chiSoHienTai >= 0),
    CONSTRAINT chk_bgcs_luongtieuthu CHECK (luongTieuThu IS NULL OR luongTieuThu >= 0),
    CONSTRAINT chk_bgcs_trangThai CHECK (trangThaiGhi IN ('Đã ghi nhận', 'Chờ xác minh ngoại lệ', 'Chưa ghi nhận/Đồng hồ lỗi'))
);

-- 15. Bảng BieuPhiDichVu: Cấu hình đơn giá, định mức các loại phí
CREATE TABLE BieuPhiDichVu (
    maBieuPhi VARCHAR(30) PRIMARY KEY,
    maKeToanCauHinh VARCHAR(30) NOT NULL,
    tenBieuPhi VARCHAR(150) NOT NULL,
    loaiDichVu VARCHAR(50) NOT NULL,
    donGiaDinhMuc NUMERIC(15,2) NOT NULL,
    donViTinh VARCHAR(30) NOT NULL,
    ngayHieuLuc DATE NOT NULL,
    trangThaiHieuLuc VARCHAR(30) DEFAULT 'Đang áp dụng' NOT NULL,
    CONSTRAINT fk_bieuphi_ketoan FOREIGN KEY (maKeToanCauHinh) 
        REFERENCES KeToan(maNV),
    CONSTRAINT chk_bieuphi_dongia CHECK (donGiaDinhMuc >= 0),
    CONSTRAINT chk_bieuphi_trangthai CHECK (trangThaiHieuLuc IN ('Đang áp dụng', 'Ngừng áp dụng'))
);

-- 16. Bảng ThongBaoPhi: Hóa đơn / Thông báo phí hàng tháng cho từng căn hộ
CREATE TABLE ThongBaoPhi (
    maThongBaoPhi VARCHAR(30) PRIMARY KEY,
    maCanHo VARCHAR(20) NOT NULL,
    maKeToan VARCHAR(30) NOT NULL,
    kyTinhPhi VARCHAR(15) NOT NULL,
    ngayPhatHanh DATE NULL,
    hanThanhToan DATE NOT NULL,
    tongTienPhaiThu NUMERIC(15,2) DEFAULT 0 NOT NULL,
    soTienDaThu NUMERIC(15,2) DEFAULT 0 NOT NULL,
    duNoConLai NUMERIC(15,2) DEFAULT 0 NOT NULL,
    trangThaiPhatHanh VARCHAR(40) DEFAULT 'Bản nháp' NOT NULL,
    trangThaiThanhToan VARCHAR(40) DEFAULT 'Chưa thanh toán' NOT NULL,
    CONSTRAINT fk_tbp_canho FOREIGN KEY (maCanHo) 
        REFERENCES CanHo(maCanHo),
    CONSTRAINT fk_tbp_ketoan FOREIGN KEY (maKeToan) 
        REFERENCES KeToan(maNV),
    CONSTRAINT uq_tbp_canho_kytinhphi UNIQUE (maCanHo, kyTinhPhi),
    CONSTRAINT chk_tbp_tongtien CHECK (tongTienPhaiThu >= 0),
    CONSTRAINT chk_tbp_dathu CHECK (soTienDaThu >= 0),
    CONSTRAINT chk_tbp_duno CHECK (duNoConLai >= 0),
    CONSTRAINT chk_tbp_ttphathanh CHECK (trangThaiPhatHanh IN ('Bản nháp', 'Đã phát hành', 'Gửi thông báo thất bại')),
    CONSTRAINT chk_tbp_ttthanhtoan CHECK (trangThaiThanhToan IN ('Chưa thanh toán', 'Đã thanh toán một phần', 'Đã thanh toán', 'Nợ đọng'))
);

-- 17. Bảng ChiTietKhoanPhi: Các khoản tiền chi tiết trong thông báo phí
CREATE TABLE ChiTietKhoanPhi (
    maChiTiet VARCHAR(30) PRIMARY KEY,
    maThongBaoPhi VARCHAR(30) NOT NULL,
    maBieuPhi VARCHAR(30) NOT NULL,
    maBanGhiChiSo VARCHAR(30) NULL UNIQUE,
    tenKhoanMuc VARCHAR(150) NOT NULL,
    soLuongTieuThu NUMERIC(12,2) NOT NULL,
    donGiaApDung NUMERIC(15,2) NOT NULL,
    thanhTien NUMERIC(15,2) NOT NULL,
    CONSTRAINT fk_ctkp_tbp FOREIGN KEY (maThongBaoPhi) 
        REFERENCES ThongBaoPhi(maThongBaoPhi) ON DELETE CASCADE,
    CONSTRAINT fk_ctkp_bieuphi FOREIGN KEY (maBieuPhi) 
        REFERENCES BieuPhiDichVu(maBieuPhi) ON DELETE RESTRICT,
    CONSTRAINT fk_ctkp_bgcs FOREIGN KEY (maBanGhiChiSo) 
        REFERENCES BangGhiChiSo(maBanGhi),
    CONSTRAINT chk_ctkp_soluong CHECK (soLuongTieuThu >= 0),
    CONSTRAINT chk_ctkp_dongia CHECK (donGiaApDung >= 0),
    CONSTRAINT chk_ctkp_thanhtien CHECK (thanhTien >= 0)
);

-- =============================================================================
-- PHẦN 4: NHÓM BẢNG TIỆN ÍCH NỘI KHU & ĐẶT CHỖ
-- (Tạo trước để GiaoDichThanhToan có thể tham chiếu maBooking)
-- =============================================================================

-- 29. Bảng TienIchNoiKhu: Danh mục tiện ích (Bể bơi, Sân thể thao, BBQ,...)
CREATE TABLE TienIchNoiKhu (
    maTienIch VARCHAR(30) PRIMARY KEY,
    maToaNha VARCHAR(20) NOT NULL,
    tenTienIch VARCHAR(150) NOT NULL,
    noiQuySuDung TEXT NOT NULL,
    sucChuaToiDa INTEGER NOT NULL,
    dinhNgachLuotTuan INTEGER NOT NULL,
    mucPhiSuDung NUMERIC(15,2) DEFAULT 0 NOT NULL,
    tienCocYeuCau NUMERIC(15,2) DEFAULT 0 NOT NULL,
    trangThaiPhucVu VARCHAR(40) DEFAULT 'Đang hoạt động',
    CONSTRAINT fk_tienich_toanha FOREIGN KEY (maToaNha) 
        REFERENCES ToaNha(maToaNha) ON DELETE RESTRICT,
    CONSTRAINT chk_tienich_succhua CHECK (sucChuaToiDa > 0),
    CONSTRAINT chk_tienich_quota CHECK (dinhNgachLuotTuan > 0),
    CONSTRAINT chk_tienich_phi CHECK (mucPhiSuDung >= 0),
    CONSTRAINT chk_tienich_tiencoc CHECK (tienCocYeuCau >= 0),
    CONSTRAINT chk_tienich_trangthai CHECK (trangThaiPhucVu IN ('Đang hoạt động', 'Đang bảo trì', 'Tạm ngưng'))
);

-- 30. Bảng KhungGioTienIch: Các khung giờ / slot hoạt động của tiện ích
CREATE TABLE KhungGioTienIch (
    maKhungGio VARCHAR(30) PRIMARY KEY,
    maTienIch VARCHAR(30) NOT NULL,
    ngaySuDung DATE NOT NULL,
    gioBatDau TIME NOT NULL,
    gioKetThuc TIME NOT NULL,
    soSlotConTrong INTEGER NOT NULL,
    trangThaiKhungGio VARCHAR(40) DEFAULT 'Còn trống' NOT NULL,
    CONSTRAINT fk_khunggio_tienich FOREIGN KEY (maTienIch) 
        REFERENCES TienIchNoiKhu(maTienIch) ON DELETE CASCADE,
    CONSTRAINT uq_khunggio_tienich_slot UNIQUE (maTienIch, ngaySuDung, gioBatDau),
    CONSTRAINT chk_khunggio_thoigian CHECK (gioKetThuc > gioBatDau),
    CONSTRAINT chk_khunggio_slot CHECK (soSlotConTrong >= 0),
    CONSTRAINT chk_khunggio_trangthai CHECK (trangThaiKhungGio IN ('Còn trống', 'Đang giữ chỗ tạm thời', 'Đã khóa (Occupied)', 'Tạm dừng bảo trì'))
);

-- 31. Bảng PhieuDatChoTienIch: Đặt chỗ của cư dân
CREATE TABLE PhieuDatChoTienIch (
    maBooking VARCHAR(30) PRIMARY KEY,
    maKhungGio VARCHAR(30) NOT NULL,
    maCuDan VARCHAR(30) NOT NULL,
    maCanHo VARCHAR(20) NOT NULL,
    soNguoiThamGia INTEGER DEFAULT 1 NOT NULL,
    chiPhiPhatSinh NUMERIC(15,2) DEFAULT 0 NOT NULL,
    maQRCheckIn VARCHAR(100) NULL UNIQUE,
    thoiGianDat TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    hanGiuChoTamThoi TIMESTAMP NULL,
    trangThaiBooking VARCHAR(40) DEFAULT 'Đã xác nhận' NOT NULL,
    CONSTRAINT fk_booking_khunggio FOREIGN KEY (maKhungGio) 
        REFERENCES KhungGioTienIch(maKhungGio) ON DELETE RESTRICT,
    CONSTRAINT fk_booking_cudan FOREIGN KEY (maCuDan) 
        REFERENCES CuDan(maCuDan),
    CONSTRAINT fk_booking_canho FOREIGN KEY (maCanHo) 
        REFERENCES CanHo(maCanHo),
    CONSTRAINT chk_booking_songuoi CHECK (soNguoiThamGia > 0),
    CONSTRAINT chk_booking_chiphi CHECK (chiPhiPhatSinh >= 0),
    CONSTRAINT chk_booking_trangthai CHECK (trangThaiBooking IN ('Chờ thanh toán cọc', 'Đã xác nhận', 'Đã sử dụng (Checked-in)', 'Đã hủy'))
);

-- =============================================================================
-- PHẦN 5: NHÓM BẢNG GIAO DỊCH THANH TOÁN & BIÊN NHẬN
-- =============================================================================

-- 18. Bảng GiaoDichThanhToan: Giao dịch thanh toán phí hóa đơn hoặc phí tiện ích
CREATE TABLE GiaoDichThanhToan (
    maGiaoDich VARCHAR(30) PRIMARY KEY,
    maThongBaoPhi VARCHAR(30) NULL,
    maBooking VARCHAR(30) NULL,
    maCuDan VARCHAR(30) NOT NULL,
    maKeToanDoiSoat VARCHAR(30) NULL,
    maThamChieuHoaDon VARCHAR(50) NOT NULL,
    phuongThucThanhToan VARCHAR(50) NOT NULL,
    soTienGiaoDich NUMERIC(15,2) NOT NULL,
    thoiGianGiaoDich TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    trangThaiGiaoDich VARCHAR(40) DEFAULT 'Đang xác minh' NOT NULL,
    ketQuaDoiSoat VARCHAR(40) DEFAULT 'Chờ đối soát' NOT NULL,
    thoiGianDoiSoat TIMESTAMP NULL,
    CONSTRAINT fk_gdt_tbp FOREIGN KEY (maThongBaoPhi) 
        REFERENCES ThongBaoPhi(maThongBaoPhi),
    CONSTRAINT fk_gdt_booking FOREIGN KEY (maBooking) 
        REFERENCES PhieuDatChoTienIch(maBooking),
    CONSTRAINT fk_gdt_cudan FOREIGN KEY (maCuDan) 
        REFERENCES CuDan(maCuDan),
    CONSTRAINT fk_gdt_ketoan FOREIGN KEY (maKeToanDoiSoat) 
        REFERENCES KeToan(maNV),
    CONSTRAINT chk_gdt_sotien CHECK (soTienGiaoDich > 0),
    CONSTRAINT chk_gdt_phuongthuc CHECK (phuongThucThanhToan IN ('Trực tuyến (Cổng thanh toán)', 'Tiền mặt', 'Chuyển khoản')),
    CONSTRAINT chk_gdt_ttgiaodich CHECK (trangThaiGiaoDich IN ('Đang xác minh', 'Thành công', 'Thất bại', 'Đã hủy')),
    CONSTRAINT chk_gdt_kqdoisoat CHECK (ketQuaDoiSoat IN ('Chờ đối soát', 'Khớp', 'Chênh lệch', 'Không tìm thấy hóa đơn'))
);

-- 19. Bảng BienNhanDienTu: Biên lai / chứng từ điện tử sau thanh toán thành công
CREATE TABLE BienNhanDienTu (
    maBienNhan VARCHAR(30) PRIMARY KEY,
    maGiaoDich VARCHAR(30) NOT NULL UNIQUE,
    ngayCap TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    soTienXacNhan NUMERIC(15,2) NOT NULL,
    noiDungBienNhan TEXT NOT NULL,
    CONSTRAINT fk_biennhan_giaodich FOREIGN KEY (maGiaoDich) 
        REFERENCES GiaoDichThanhToan(maGiaoDich) ON DELETE RESTRICT,
    CONSTRAINT chk_biennhan_sotien CHECK (soTienXacNhan > 0)
);

-- =============================================================================
-- PHẦN 6: NHÓM BẢNG THIẾT BỊ KỸ THUẬT & BẢO TRÌ PHÒNG NGỪA
-- =============================================================================

-- 23. Bảng ThietBiKyThuat: Tài sản và hạ tầng kỹ thuật của tòa nhà
CREATE TABLE ThietBiKyThuat (
    maThietBi VARCHAR(30) PRIMARY KEY,
    maToaNha VARCHAR(20) NOT NULL,
    tenThietBi VARCHAR(150) NOT NULL,
    nhomHeThong VARCHAR(50) NOT NULL,
    viTriLapDat VARCHAR(150) NOT NULL,
    thongSoTieuChuan TEXT NOT NULL,
    trangThaiThietBi VARCHAR(40) DEFAULT 'Bình thường' NOT NULL,
    CONSTRAINT fk_thietbi_toanha FOREIGN KEY (maToaNha) 
        REFERENCES ToaNha(maToaNha) ON DELETE RESTRICT,
    CONSTRAINT chk_thietbi_hethong CHECK (nhomHeThong IN ('PCCC', 'Thang máy', 'Máy phát điện', 'Cấp thoát nước')),
    CONSTRAINT chk_thietbi_trangthai CHECK (trangThaiThietBi IN ('Bình thường', 'Đang bảo trì', 'Cảnh báo sự cố', 'Ngừng hoạt động'))
);

-- 24. Bảng MauChecklist: Mẫu danh mục kiểm tra kỹ thuật
CREATE TABLE MauChecklist (
    maChecklist VARCHAR(30) PRIMARY KEY,
    tenChecklist VARCHAR(200) NOT NULL,
    nhomHeThongApDung VARCHAR(50) NOT NULL,
    CONSTRAINT chk_mauchecklist_hethong CHECK (nhomHeThongApDung IN ('PCCC', 'Thang máy', 'Máy phát điện', 'Cấp thoát nước'))
);

-- 25. Bảng HangMucChecklist: Chi tiết từng hạng mục cần kiểm tra
CREATE TABLE HangMucChecklist (
    maHangMuc VARCHAR(30) PRIMARY KEY,
    maChecklist VARCHAR(30) NOT NULL,
    tenHangMucKiemTra VARCHAR(255) NOT NULL,
    thongSoChuan VARCHAR(150) NOT NULL,
    batBuocDoDac BOOLEAN DEFAULT TRUE NOT NULL,
    CONSTRAINT fk_hangmuc_checklist FOREIGN KEY (maChecklist) 
        REFERENCES MauChecklist(maChecklist) ON DELETE CASCADE
);

-- 26. Bảng KeHoachBaoTri: Kế hoạch kiểm tra bảo dưỡng định kỳ
CREATE TABLE KeHoachBaoTri (
    maKeHoach VARCHAR(30) PRIMARY KEY,
    maThietBi VARCHAR(30) NOT NULL,
    maChecklist VARCHAR(30) NOT NULL,
    maNVBQL VARCHAR(30) NOT NULL,
    tenKeHoach VARCHAR(200) NOT NULL,
    chuKyLap VARCHAR(30) NOT NULL,
    ngayBatDau DATE NOT NULL,
    ngayDenHanTiepTheo DATE NOT NULL,
    trangThaiKeHoach VARCHAR(40) DEFAULT 'Đang kích hoạt' NOT NULL,
    CONSTRAINT fk_khbt_thietbi FOREIGN KEY (maThietBi) 
        REFERENCES ThietBiKyThuat(maThietBi) ON DELETE RESTRICT,
    CONSTRAINT fk_khbt_checklist FOREIGN KEY (maChecklist) 
        REFERENCES MauChecklist(maChecklist) ON DELETE RESTRICT,
    CONSTRAINT fk_khbt_nvbql FOREIGN KEY (maNVBQL) 
        REFERENCES NhanVienBQL(maNV),
    CONSTRAINT chk_khbt_chuky CHECK (chuKyLap IN ('Hàng tuần', 'Hàng tháng', 'Hàng quý', 'Hàng năm')),
    CONSTRAINT chk_khbt_ngay CHECK (ngayDenHanTiepTheo >= ngayBatDau),
    CONSTRAINT chk_khbt_trangthai CHECK (trangThaiKeHoach IN ('Đang kích hoạt', 'Tạm dừng', 'Đã kết thúc'))
);

-- 27. Bảng PhieuCongViecBaoTri: Phiếu công việc kiểm tra bảo trì định kỳ
CREATE TABLE PhieuCongViecBaoTri (
    maPhieuCV VARCHAR(30) PRIMARY KEY,
    maKeHoach VARCHAR(30) NOT NULL,
    maNVKyThuat VARCHAR(30) NOT NULL,
    maNVBQLNghiemThu VARCHAR(30) NULL,
    thoiGianGiaoViec TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    thoiGianTiepNhan TIMESTAMP NULL,
    ngayDenHan DATE NOT NULL,
    ngayHoanThanh TIMESTAMP NULL,
    anhMinhChung VARCHAR(255) NULL,
    trangThaiPhieu VARCHAR(40) DEFAULT 'Mới khởi tạo' NOT NULL,
    lyDoYeuCauKiemTraLai TEXT NULL,
    nhanXetNghiemThu TEXT NULL,
    CONSTRAINT fk_pcv_kehoach FOREIGN KEY (maKeHoach) 
        REFERENCES KeHoachBaoTri(maKeHoach) ON DELETE RESTRICT,
    CONSTRAINT fk_pcv_nvkt FOREIGN KEY (maNVKyThuat) 
        REFERENCES NhanVienKyThuat(maNV),
    CONSTRAINT fk_pcv_nvbql FOREIGN KEY (maNVBQLNghiemThu) 
        REFERENCES NhanVienBQL(maNV),
    CONSTRAINT chk_pcv_trangthai CHECK (trangThaiPhieu IN ('Mới khởi tạo', 'Đang thực hiện', 'Chờ nghiệm thu', 'Yêu cầu kiểm tra bổ sung', 'Đã hoàn thành', 'Hoàn thành có cảnh báo'))
);

-- 28. Bảng KetQuaHangMucChecklist: Kết quả đo đạc kiểm tra từng hạng mục
CREATE TABLE KetQuaHangMucChecklist (
    maKetQua VARCHAR(30) PRIMARY KEY,
    maPhieuCV VARCHAR(30) NOT NULL,
    maHangMuc VARCHAR(30) NOT NULL,
    thongSoThucTe VARCHAR(150) NULL,
    danhGiaDat BOOLEAN DEFAULT TRUE NOT NULL,
    nguyCoHuHongCao BOOLEAN DEFAULT FALSE NOT NULL,
    ghiChuHienTruong TEXT NULL,
    CONSTRAINT fk_kqhm_phieucv FOREIGN KEY (maPhieuCV) 
        REFERENCES PhieuCongViecBaoTri(maPhieuCV) ON DELETE CASCADE,
    CONSTRAINT fk_kqhm_hangmuc FOREIGN KEY (maHangMuc) 
        REFERENCES HangMucChecklist(maHangMuc),
    CONSTRAINT uq_kqhm_phieucv_hangmuc UNIQUE (maPhieuCV, maHangMuc)
);

-- =============================================================================
-- PHẦN 7: NHÓM BẢNG SERVICE DESK, YÊU CẦU HỖ TRỢ & SLA
-- =============================================================================

-- 20. Bảng QuyDinhSLA: Khung thời gian cam kết dịch vụ theo mức ưu tiên
CREATE TABLE QuyDinhSLA (
    maSLA VARCHAR(30) PRIMARY KEY,
    phanLoaiSuCo VARCHAR(50) NOT NULL,
    mucDoUuTien VARCHAR(30) NOT NULL,
    thoiGianCamKetGio NUMERIC(6,2) NOT NULL,
    CONSTRAINT uq_sla_phanloai_mucdo UNIQUE (phanLoaiSuCo, mucDoUuTien),
    CONSTRAINT chk_sla_phanloai CHECK (phanLoaiSuCo IN ('Kỹ thuật', 'An ninh', 'Vệ sinh', 'Dịch vụ')),
    CONSTRAINT chk_sla_mucdo CHECK (mucDoUuTien IN ('Khẩn cấp', 'Cao', 'Trung bình', 'Thấp')),
    CONSTRAINT chk_sla_thoigian CHECK (thoiGianCamKetGio > 0)
);

-- 21. Bảng PhieuYeuCauHoTro: Phiếu phản ánh sự cố / yêu cầu hỗ trợ (Ticket)
CREATE TABLE PhieuYeuCauHoTro (
    maTicket VARCHAR(30) PRIMARY KEY,
    maCuDan VARCHAR(30) NULL,
    maCanHo VARCHAR(20) NULL,
    maNVBQLDieuPhoi VARCHAR(30) NULL,
    maNVKyThuat VARCHAR(30) NULL,
    maSLA VARCHAR(30) NULL,
    maPhieuCVPhatSinh VARCHAR(30) NULL,
    maThietBi VARCHAR(30) NULL,
    phanLoaiSuCo VARCHAR(50) NOT NULL,
    tieuDe VARCHAR(200) NOT NULL,
    moTaChiTiet TEXT NOT NULL,
    mucDoUuTien VARCHAR(30) DEFAULT 'Trung bình',
    thoiGianGui TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    hanHoanThanhSLA TIMESTAMP NULL,
    thoiGianTamHoan TIMESTAMP NULL,
    thoiGianHoanThanh TIMESTAMP NULL,
    trangThaiTicket VARCHAR(40) DEFAULT 'Chờ tiếp nhận' NOT NULL,
    ketQuaTuanThuSLA VARCHAR(30) NULL,
    lyDoTuChoiHoacTamHoan TEXT NULL,
    ketQuaNghiemThu TEXT NULL,
    diemDanhGiaHaiLong INTEGER NULL,
    nhanXetDanhGia TEXT NULL,
    CONSTRAINT fk_ticket_cudan FOREIGN KEY (maCuDan) 
        REFERENCES CuDan(maCuDan),
    CONSTRAINT fk_ticket_canho FOREIGN KEY (maCanHo) 
        REFERENCES CanHo(maCanHo),
    CONSTRAINT fk_ticket_nvbql FOREIGN KEY (maNVBQLDieuPhoi) 
        REFERENCES NhanVienBQL(maNV),
    CONSTRAINT fk_ticket_nvkt FOREIGN KEY (maNVKyThuat) 
        REFERENCES NhanVienKyThuat(maNV),
    CONSTRAINT fk_ticket_sla FOREIGN KEY (maSLA) 
        REFERENCES QuyDinhSLA(maSLA),
    CONSTRAINT fk_ticket_pcv FOREIGN KEY (maPhieuCVPhatSinh) 
        REFERENCES PhieuCongViecBaoTri(maPhieuCV),
    CONSTRAINT fk_ticket_thietbi FOREIGN KEY (maThietBi) 
        REFERENCES ThietBiKyThuat(maThietBi),
    CONSTRAINT chk_ticket_phanloai CHECK (phanLoaiSuCo IN ('Kỹ thuật', 'An ninh', 'Vệ sinh', 'Dịch vụ')),
    CONSTRAINT chk_ticket_mucdo CHECK (mucDoUuTien IN ('Khẩn cấp', 'Cao', 'Trung bình', 'Thấp')),
    CONSTRAINT chk_ticket_trangthai CHECK (trangThaiTicket IN ('Chờ tiếp nhận', 'Từ chối tiếp nhận', 'Đã giao việc', 'Đang xử lý', 'Tạm hoãn / Chờ vật tư', 'Đã hoàn thành')),
    CONSTRAINT chk_ticket_sla_result CHECK (ketQuaTuanThuSLA IN ('Đúng hạn', 'Quá hạn SLA')),
    CONSTRAINT chk_ticket_diem CHECK (diemDanhGiaHaiLong BETWEEN 1 AND 5)
);

-- =============================================================================
-- PHẦN 8: NHÓM BẢNG TRUYỀN THÔNG, KHẢO SÁT & TỆP ĐÍNH KÈM
-- =============================================================================

-- 32. Bảng ThongBaoVanHanh: Bản tin, thông báo vận hành tòa nhà
CREATE TABLE ThongBaoVanHanh (
    maThongBao VARCHAR(30) PRIMARY KEY,
    maNVBQL VARCHAR(30) NOT NULL,
    tieuDe VARCHAR(250) NOT NULL,
    phanLoaiTin VARCHAR(50) NOT NULL,
    mucDoQuanTrong VARCHAR(30) DEFAULT 'Bình thường' NOT NULL,
    noiDungChiTiet TEXT NOT NULL,
    phamViNguoiNhan VARCHAR(150) NOT NULL,
    kenhPhanPhoi VARCHAR(100) NOT NULL,
    thoiGianLenLich TIMESTAMP NULL,
    thoiGianPhatHanh TIMESTAMP NULL,
    trangThaiThongBao VARCHAR(30) DEFAULT 'Bản nháp' NOT NULL,
    CONSTRAINT fk_tbvh_nvbql FOREIGN KEY (maNVBQL) 
        REFERENCES NhanVienBQL(maNV),
    CONSTRAINT chk_tbvh_phanloai CHECK (phanLoaiTin IN ('Lịch cắt điện/nước', 'Kỹ thuật - Bảo trì', 'PCCC', 'Nội quy', 'Khác')),
    CONSTRAINT chk_tbvh_mucdo CHECK (mucDoQuanTrong IN ('Bình thường', 'Quan trọng', 'Khẩn cấp')),
    CONSTRAINT chk_tbvh_trangthai CHECK (trangThaiThongBao IN ('Bản nháp', 'Đã lên lịch', 'Đã phát hành'))
);

-- 22. Bảng TepDinhKem: Tệp đa phương tiện đính kèm ticket hoặc thông báo
CREATE TABLE TepDinhKem (
    maTep VARCHAR(30) PRIMARY KEY,
    maTicket VARCHAR(30) NULL,
    maThongBao VARCHAR(30) NULL,
    tenTep VARCHAR(255) NOT NULL,
    dinhDang VARCHAR(20) NOT NULL,
    dungLuongMB NUMERIC(5,2) NOT NULL,
    duongDanLuuTru VARCHAR(500) NOT NULL,
    vaiTroTep VARCHAR(50) NOT NULL,
    CONSTRAINT fk_tep_ticket FOREIGN KEY (maTicket) 
        REFERENCES PhieuYeuCauHoTro(maTicket) ON DELETE CASCADE,
    CONSTRAINT fk_tep_thongbao FOREIGN KEY (maThongBao) 
        REFERENCES ThongBaoVanHanh(maThongBao) ON DELETE CASCADE,
    CONSTRAINT chk_tep_dungluong CHECK (dungLuongMB > 0 AND dungLuongMB <= 20.00),
    CONSTRAINT chk_tep_vaitro CHECK (vaiTroTep IN ('Ảnh hiện trạng sự cố', 'Ảnh nghiệm thu sau xử lý', 'Tệp thông báo')),
    CONSTRAINT chk_tep_xor CHECK ((maTicket IS NOT NULL AND maThongBao IS NULL) OR (maTicket IS NULL AND maThongBao IS NOT NULL))
);

-- 33. Bảng TrangThaiNhanThongBao: Theo dõi phân phối và lượt đọc tin của cư dân
CREATE TABLE TrangThaiNhanThongBao (
    maThongBao VARCHAR(30) NOT NULL,
    maCuDan VARCHAR(30) NOT NULL,
    thoiGianPhanPhoi TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    daDoc BOOLEAN DEFAULT FALSE NOT NULL,
    thoiGianDoc TIMESTAMP NULL,
    PRIMARY KEY (maThongBao, maCuDan),
    CONSTRAINT fk_ttntb_thongbao FOREIGN KEY (maThongBao) 
        REFERENCES ThongBaoVanHanh(maThongBao) ON DELETE CASCADE,
    CONSTRAINT fk_ttntb_cudan FOREIGN KEY (maCuDan) 
        REFERENCES CuDan(maCuDan) ON DELETE CASCADE
);

-- 34. Bảng DotKhaoSat: Các đợt khảo sát, lấy ý kiến, biểu quyết
CREATE TABLE DotKhaoSat (
    maKhaoSat VARCHAR(30) PRIMARY KEY,
    maNVBQL VARCHAR(30) NOT NULL,
    tieuDe VARCHAR(250) NOT NULL,
    mucDichMoTa TEXT NOT NULL,
    phamViDoiTuong VARCHAR(150) NOT NULL,
    ngayBatDau TIMESTAMP NOT NULL,
    ngayKetThuc TIMESTAMP NOT NULL,
    quyTacGioiHanPhieu VARCHAR(50) DEFAULT '1 phiếu/căn hộ' NOT NULL,
    cheDoDanhTinh VARCHAR(30) DEFAULT 'Công khai' NOT NULL,
    trangThaiKhaoSat VARCHAR(30) DEFAULT 'Đã lên lịch' NOT NULL,
    CONSTRAINT fk_khaosat_nvbql FOREIGN KEY (maNVBQL) 
        REFERENCES NhanVienBQL(maNV),
    CONSTRAINT chk_khaosat_thoigian CHECK (ngayKetThuc > ngayBatDau),
    CONSTRAINT chk_khaosat_quytac CHECK (quyTacGioiHanPhieu IN ('1 phiếu/căn hộ', '1 phiếu/tài khoản')),
    CONSTRAINT chk_khaosat_chedo CHECK (cheDoDanhTinh IN ('Công khai', 'Ẩn danh')),
    CONSTRAINT chk_khaosat_trangthai CHECK (trangThaiKhaoSat IN ('Đã lên lịch', 'Đang mở', 'Đã đóng'))
);

-- 35. Bảng CauHoiKhaoSat: Danh mục câu hỏi trong đợt khảo sát
CREATE TABLE CauHoiKhaoSat (
    maCauHoi VARCHAR(30) PRIMARY KEY,
    maKhaoSat VARCHAR(30) NOT NULL,
    noiDungCauHoi TEXT NOT NULL,
    dangCauHoi VARCHAR(50) NOT NULL,
    batBuocTraLoi BOOLEAN DEFAULT TRUE NOT NULL,
    CONSTRAINT fk_cauhoi_khaosat FOREIGN KEY (maKhaoSat) 
        REFERENCES DotKhaoSat(maKhaoSat) ON DELETE CASCADE,
    CONSTRAINT chk_cauhoi_dang CHECK (dangCauHoi IN ('Trắc nghiệm đơn lựa chọn', 'Trắc nghiệm nhiều lựa chọn', 'Thang điểm 1-5 sao', 'Ý kiến tự do'))
);

-- 36. Bảng PhieuPhanHoiKhaoSat: Câu trả lời / phiếu bình chọn của cư dân
CREATE TABLE PhieuPhanHoiKhaoSat (
    maPhanHoi VARCHAR(30) PRIMARY KEY,
    maKhaoSat VARCHAR(30) NOT NULL,
    maCuDan VARCHAR(30) NOT NULL,
    maCanHo VARCHAR(20) NOT NULL,
    thoiGianNop TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    duLieuTraLoi TEXT NOT NULL, -- Định dạng JSONB / TEXT cấu trúc phản hồi
    CONSTRAINT fk_phanhoi_khaosat FOREIGN KEY (maKhaoSat) 
        REFERENCES DotKhaoSat(maKhaoSat) ON DELETE RESTRICT,
    CONSTRAINT fk_phanhoi_cudan FOREIGN KEY (maCuDan) 
        REFERENCES CuDan(maCuDan),
    CONSTRAINT fk_phanhoi_canho FOREIGN KEY (maCanHo) 
        REFERENCES CanHo(maCanHo),
    CONSTRAINT uq_phanhoi_khaosat_cudan UNIQUE (maKhaoSat, maCuDan)
);

-- =============================================================================
-- PHẦN 9: NHÓM BẢNG BÁO CÁO THỐNG KÊ & NHẬT KÝ HỆ THỐNG (AUDIT LOG)
-- =============================================================================

-- 37. Bảng BaoCaoThongKe: Lịch sử kết xuất báo cáo KPI và dữ liệu vận hành
CREATE TABLE BaoCaoThongKe (
    maBaoCao VARCHAR(30) PRIMARY KEY,
    maNguoiDung VARCHAR(30) NOT NULL,
    loaiBaoCao VARCHAR(100) NOT NULL,
    tuNgay DATE NOT NULL,
    denNgay DATE NOT NULL,
    phamViThongKe VARCHAR(100) NOT NULL,
    dinhDangKetXuat VARCHAR(15) NOT NULL,
    duongDanFileKetXuat VARCHAR(500) NULL,
    thoiGianKetXuat TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_baocao_nguoidung FOREIGN KEY (maNguoiDung) 
        REFERENCES NguoiDungHeThong(maNguoiDung),
    CONSTRAINT chk_baocao_loai CHECK (loaiBaoCao IN ('Báo cáo Doanh thu & Công nợ', 'Báo cáo Hiệu suất xử lý SLA', 'Báo cáo Chi phí OPEX & Bảo dưỡng tài sản')),
    CONSTRAINT chk_baocao_thoigian CHECK (denNgay >= tuNgay),
    CONSTRAINT chk_baocao_dinhdang CHECK (dinhDangKetXuat IN ('Excel', 'PDF'))
);

-- 38. Bảng NhatKyHeThong: Nhật ký kiểm toán an toàn thông tin (Audit Log)
CREATE TABLE NhatKyHeThong (
    maLog VARCHAR(30) PRIMARY KEY,
    maNguoiDung VARCHAR(30) NOT NULL,
    loaiThaoTac VARCHAR(100) NOT NULL,
    doiTuongTacDong VARCHAR(150) NOT NULL,
    thoiGianGhi TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    chiTietThayDoi TEXT NULL,
    CONSTRAINT fk_nhatky_nguoidung FOREIGN KEY (maNguoiDung) 
        REFERENCES NguoiDungHeThong(maNguoiDung) ON DELETE RESTRICT
);

-- =============================================================================
-- PHẦN 10: TỐI ƯU HÓA CHỈ MỤC (INDEXES) PHỤC VỤ TRUY VẤN TẦNG DAM
-- =============================================================================

CREATE INDEX idx_canho_matang ON CanHo(maTang);
CREATE INDEX idx_hsct_canho_cudan ON HoSoCuTru(maCanHo, maCuDan);
CREATE INDEX idx_phuongtien_canho ON PhuongTien(maCanHo);
CREATE INDEX idx_dongho_canho ON DongHoTieuThu(maCanHo);
CREATE INDEX idx_dongho_khuvuc ON DongHoTieuThu(maKhuVuc);
CREATE INDEX idx_bgcs_dongho_ky ON BangGhiChiSo(maDongHo, kyGhiChiSo);
CREATE INDEX idx_tbp_canho_ky ON ThongBaoPhi(maCanHo, kyTinhPhi);
CREATE INDEX idx_tbp_trangthai ON ThongBaoPhi(trangThaiThanhToan, trangThaiPhatHanh);
CREATE INDEX idx_ctkp_tbp ON ChiTietKhoanPhi(maThongBaoPhi);
CREATE INDEX idx_gdt_tbp ON GiaoDichThanhToan(maThongBaoPhi);
CREATE INDEX idx_gdt_cudan ON GiaoDichThanhToan(maCuDan);
CREATE INDEX idx_ticket_cudan_canho ON PhieuYeuCauHoTro(maCuDan, maCanHo);
CREATE INDEX idx_ticket_trangthai_sla ON PhieuYeuCauHoTro(trangThaiTicket, hanHoanThanhSLA);
CREATE INDEX idx_thietbi_toanha ON ThietBiKyThuat(maToaNha);
CREATE INDEX idx_pcv_kehoach ON PhieuCongViecBaoTri(maKeHoach);
CREATE INDEX idx_khunggio_tienich_ngay ON KhungGioTienIch(maTienIch, ngaySuDung);
CREATE INDEX idx_booking_khunggio ON PhieuDatChoTienIch(maKhungGio);
CREATE INDEX idx_tbvh_trangthai ON ThongBaoVanHanh(trangThaiThongBao, thoiGianPhatHanh);
CREATE INDEX idx_khaosat_trangthai ON DotKhaoSat(trangThaiKhaoSat, ngayKetThuc);
CREATE INDEX idx_nhatky_nguoidung_thoigian ON NhatKyHeThong(maNguoiDung, thoiGianGhi);
