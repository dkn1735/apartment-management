package vn.edu.hust.chungcu.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mail.MailSendException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import vn.edu.hust.chungcu.common.email.EmailGui;
import vn.edu.hust.chungcu.common.email.EmailSender;

/**
 * SMTP lỗi không đổi phản hồi; log lỗi có mã yêu cầu, không có token, link hay email
 * (spec 0010, AC-3, AC-11).
 */
@ExtendWith(OutputCaptureExtension.class)
class QuenMatKhauSmtpLoiIntegrationTest extends QuenMatKhauTestBase {

	@MockitoBean
	EmailSender sender;

	@Test
	void smtpLoiVanTra200VaLogKhongLoBiMat(CapturedOutput output) throws Exception {
		// Lỗi SMTP thật thường chép địa chỉ người nhận vào thông điệp
		doThrow(new MailSendException("550 không có hộp thư " + this.tk.email())).when(this.sender).gui(any());

		this.mockMvc.perform(QuenMatKhauThu.yeuCau(this.tk.email()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value(QuenMatKhauThu.PHAN_HOI_CHUNG));

		org.mockito.ArgumentCaptor<EmailGui> email = org.mockito.ArgumentCaptor.forClass(EmailGui.class);
		verify(this.sender, timeout(5_000)).gui(email.capture());
		List<Map<String, Object>> dong = QuenMatKhauThu.yeuCauCua(this.jdbc, this.tk.maNguoiDung());
		assertThat(dong).hasSize(1);
		String maYeuCau = dong.get(0).get("ma_yeu_cau").toString();
		String token = email.getValue().chuThuong().replaceAll("(?s).*#token=([A-Za-z0-9_-]{43}).*", "$1");

		long han = System.currentTimeMillis() + 5_000;
		while (!output.getAll().contains("Gửi email đặt lại mật khẩu thất bại") && System.currentTimeMillis() < han) {
			Thread.sleep(50);
		}
		assertThat(output.getAll()).contains("Gửi email đặt lại mật khẩu thất bại: maYeuCau=" + maYeuCau)
			.contains(MailSendException.class.getName())
			.doesNotContain(token)
			.doesNotContain(vn.edu.hust.chungcu.auth.service.RefreshTokenGenerator.sha256Hex(token))
			.doesNotContain("dat-lai-mat-khau#token")
			.doesNotContain(this.tk.email());
		// Không gửi lại tự động
		Thread.sleep(300);
		verify(this.sender, org.mockito.Mockito.times(1)).gui(any());
	}

	@Test
	void emailKhongCoTaiKhoanChiLogLyDo(CapturedOutput output) throws Exception {
		this.mockMvc.perform(QuenMatKhauThu.yeuCau("ai-do@chungcu.test")).andExpect(status().isOk());
		assertThat(output.getAll()).contains("lyDo=KHONG_CO_TAI_KHOAN").doesNotContain("ai-do@chungcu.test");
		assertThat(new vn.edu.hust.chungcu.nhatky.NhatKyDb(this.jdbc).toanBoSau("0"))
			.doesNotContain("AUTH_YEU_CAU_DAT_LAI_MAT_KHAU" + "x");
	}

}
