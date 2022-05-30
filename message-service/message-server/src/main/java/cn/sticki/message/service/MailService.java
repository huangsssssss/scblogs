package cn.sticki.message.service;

import cn.sticki.message.pojo.Mail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Objects;

@Slf4j
@Service
public class MailService {

	@Resource
	private JavaMailSenderImpl mailSender;//注入邮件工具类

	@Async
	public void sendMail(Mail mail) throws MailSendException {
		checkMail(mail);
		try {
			sendMimeMail(mail);
		} catch (Exception e) {
			log.warn("邮件发送失败：{}", e.getMessage());
			throw new MailSendException("邮件发送失败:" + e.getMessage());
		}
	}

	//检测邮件信息类
	private void checkMail(Mail mail) throws RuntimeException {
		if (isEmpty(mail.getTo())) {
			throw new RuntimeException("邮件收信人不能为空");
		}
		if (isEmpty(mail.getSubject())) {
			throw new RuntimeException("邮件主题不能为空");
		}
		if (isEmpty(mail.getText())) {
			throw new RuntimeException("邮件内容不能为空");
		}
	}

	//构建复杂邮件信息类
	private void sendMimeMail(Mail mail) throws Exception {
		MimeMessageHelper messageHelper = new MimeMessageHelper(mailSender.createMimeMessage(), true);//true表示支持复杂类型
		if (mail.getFrom() == null || mail.getFrom().isEmpty())
			mail.setFrom("校园博客");
		messageHelper.setFrom(mailSender.getUsername() + '(' + mail.getFrom() + ')');//邮件发信人
		messageHelper.setTo(mail.getTo().split(","));//邮件收信人
		messageHelper.setSubject(mail.getSubject());//邮件主题
		messageHelper.setText(mail.getText());//邮件内容
		if (!isEmpty(mail.getCc())) {//抄送
			messageHelper.setCc(mail.getCc().split(","));
		}
		if (!isEmpty(mail.getBcc())) {//密送
			messageHelper.setCc(mail.getBcc().split(","));
		}
		if (mail.getMultipartFiles() != null) {//添加邮件附件
			for (MultipartFile multipartFile : mail.getMultipartFiles()) {
				messageHelper.addAttachment(Objects.requireNonNull(multipartFile.getOriginalFilename()), multipartFile);
			}
		}
		if (isEmpty(mail.getSentDate())) {
			mail.setSentDate(new Date()); //发送时间
		}
		messageHelper.setSentDate(mail.getSentDate());
		mailSender.send(messageHelper.getMimeMessage());//正式发送邮件
	}

	//保存邮件
	private void saveMail(Mail mail) {
		//将邮件保存到数据库..
	}

	private boolean isEmpty(Object str) {
		return (str == null || "".equals(str));
	}

}