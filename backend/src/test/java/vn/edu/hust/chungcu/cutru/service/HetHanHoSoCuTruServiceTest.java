package vn.edu.hust.chungcu.cutru.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import vn.edu.hust.chungcu.canho.service.CanHoCuTruService;
import vn.edu.hust.chungcu.cutru.repository.HoSoCuTruGhi;
import vn.edu.hust.chungcu.cutru.repository.HoSoCuTruQuery;
import vn.edu.hust.chungcu.nhatky.dto.DoiTuongTacDong;
import vn.edu.hust.chungcu.nhatky.dto.LoaiThaoTac;
import vn.edu.hust.chungcu.nhatky.dto.SuKienNhatKy;
import vn.edu.hust.chungcu.nhatky.service.NhatKyService;

/**
 * Kiểm thử đơn vị cho xử lý hết hạn hồ sơ cư trú và job đầu ngày (spec 0014, AC-11,
 * AC-12).
 */
class HetHanHoSoCuTruServiceTest {

	private static final Instant LUC = Instant.parse("2026-11-15T17:05:00Z");

	private static final Clock DONG_HO = Clock.fixed(LUC, ZoneId.of("Asia/Ho_Chi_Minh"));

	private static final LocalDate HOM_NAY = LocalDate.of(2026, 11, 16);

	private static final PlatformTransactionManager TX = new PlatformTransactionManager() {
		@Override
		public TransactionStatus getTransaction(TransactionDefinition definition) {
			return new SimpleTransactionStatus();
		}

		@Override
		public void commit(TransactionStatus status) {
		}

		@Override
		public void rollback(TransactionStatus status) {
		}
	};

	// covers: AC-11, AC-12
	@Test
	void loiOMotCanKhongLamDungCacCanConLai() {
		HoSoCuTruQuery query = mock(HoSoCuTruQuery.class);
		HoSoCuTruGhi ghi = mock(HoSoCuTruGhi.class);
		CanHoCuTruService canHo = mock(CanHoCuTruService.class);
		DongBoTrangThaiCanHo dongBo = mock(DongBoTrangThaiCanHo.class);
		NhatKyService nhatKy = mock(NhatKyService.class);

		when(query.maCanHoCoHoSoQuaHan(HOM_NAY)).thenReturn(List.of("A-0101", "A-0102"));
		when(canHo.khoaVaDocTrangThai("A-0101")).thenThrow(new IllegalStateException("Lỗi khóa dòng giả lập"));
		when(canHo.khoaVaDocTrangThai("A-0102")).thenReturn(Optional.of("Đang sử dụng"));
		when(ghi.hetHan("A-0102", HOM_NAY, LUC))
			.thenReturn(List.of(new HoSoCuTruGhi.DaHetHan("HS00000009", "CD00000002", LocalDate.of(2026, 11, 15))));
		when(dongBo.dongBo("A-0102", "Đang sử dụng", HOM_NAY))
			.thenReturn(new ChiTietCuTru.ThayDoi<>("Đang sử dụng", "Trống"));

		HetHanHoSoCuTruService service = new HetHanHoSoCuTruService(query, ghi, canHo, dongBo, nhatKy, DONG_HO, TX);

		assertThat(service.chay()).isEqualTo(1);
		verify(ghi, never()).hetHan(eq("A-0101"), any(), any());
		ArgumentCaptor<SuKienNhatKy> suKien = ArgumentCaptor.forClass(SuKienNhatKy.class);
		verify(nhatKy).ghi(suKien.capture());
		assertThat(suKien.getValue().loai()).isEqualTo(LoaiThaoTac.CUTRU_KET_THUC_HO_SO);
		assertThat(suKien.getValue().maNguoiThucHien()).isNull();
		assertThat(suKien.getValue().doiTuong()).isEqualTo(DoiTuongTacDong.hoSoCuTru("HS00000009"));
	}

	// covers: AC-11
	@Test
	void khongConDongQuaHanSauKhiKhoaThiBoQuaDongBoVaNhatKy() {
		HoSoCuTruQuery query = mock(HoSoCuTruQuery.class);
		HoSoCuTruGhi ghi = mock(HoSoCuTruGhi.class);
		CanHoCuTruService canHo = mock(CanHoCuTruService.class);
		DongBoTrangThaiCanHo dongBo = mock(DongBoTrangThaiCanHo.class);
		NhatKyService nhatKy = mock(NhatKyService.class);

		when(query.maCanHoCoHoSoQuaHan(HOM_NAY)).thenReturn(List.of("A-0101", "A-0102"));
		when(canHo.khoaVaDocTrangThai("A-0101")).thenReturn(Optional.empty());
		when(canHo.khoaVaDocTrangThai("A-0102")).thenReturn(Optional.of("Đang sử dụng"));
		when(ghi.hetHan("A-0102", HOM_NAY, LUC)).thenReturn(List.of());

		HetHanHoSoCuTruService service = new HetHanHoSoCuTruService(query, ghi, canHo, dongBo, nhatKy, DONG_HO, TX);

		assertThat(service.chay()).isZero();
		verify(ghi, never()).hetHan(eq("A-0101"), any(), any());
		verifyNoInteractions(dongBo, nhatKy);
	}

	// covers: AC-11
	@Test
	void jobChayBuLucKhoiDongVaChiuLoiBatThuong() {
		HetHanHoSoCuTruService hetHan = mock(HetHanHoSoCuTruService.class);
		when(hetHan.chay()).thenReturn(2);
		HoSoCuTruHetHanJob job = new HoSoCuTruHetHanJob(hetHan);

		job.chayBu();
		verify(hetHan).chay();

		doThrow(new IllegalStateException("Mất kết nối DB")).when(hetHan).chay();
		assertThatCode(job::chay).doesNotThrowAnyException();
	}

}
