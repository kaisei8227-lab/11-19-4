package com.example.eventreserve.service;

import com.example.eventreserve.entity.Member;
import com.example.eventreserve.entity.Role;
import com.example.eventreserve.repository.MemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

@Service
public class MemberService {

    private static final Logger log = LoggerFactory.getLogger(MemberService.class);

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public MemberService(MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 新規会員登録。メールアドレスとハッシュ化したパスワードをDBに保管する。
     * companyName は企業のときだけ使う（学生のときは保存しない）。
     */
    @Transactional
    public Member register(String email, String rawPassword, Role role, String companyName) {
        String normalized = normalize(email);
        if (memberRepository.existsByEmail(normalized)) {
            throw new BusinessException("このメールアドレスはすでに登録されています");
        }
        String hash = passwordEncoder.encode(rawPassword);
        String company = (role == Role.COMPANY && companyName != null) ? companyName.trim() : null;
        try {
            // saveAndFlush でこの場ですぐDBに書き込み、一意制約の違反をここで受け止める
            return memberRepository.saveAndFlush(new Member(normalized, hash, role, company));
        } catch (DataIntegrityViolationException e) {
            // ほぼ同時に同じメールアドレスで登録された場合（上の existsByEmail をすり抜けたとき）
            throw new BusinessException("このメールアドレスはすでに登録されています");
        }
    }

    /**
     * ログイン判定。
     * メールアドレスが登録されていて、パスワードが一致し、
     * さらに選んだ種類（学生／企業）と登録した種類が同じときだけ会員を返す。
     */
    @Transactional(readOnly = true)
    public Optional<Member> authenticate(String email, String rawPassword, Role role) {
        String normalized = normalize(email);
        Optional<Member> found = memberRepository.findByEmail(normalized);
        // 失敗した理由はサーバーのログ（Eclipseのコンソール / Renderの Logs）にだけ出す。画面には出さない
        if (found.isEmpty()) {
            log.info("ログイン失敗: メールアドレスが登録されていません email={} (登録会員数={})",
                    normalized, memberRepository.count());
            return Optional.empty();
        }
        Member m = found.get();
        if (!passwordEncoder.matches(rawPassword, m.getPasswordHash())) {
            log.info("ログイン失敗: パスワードが違います email={}", normalized);
            return Optional.empty();
        }
        if (m.getRole() != role) {
            log.info("ログイン失敗: 種類が違います email={} 登録={} 選択={}", normalized, m.getRole(), role);
            return Optional.empty();
        }
        return found;
    }

    @Transactional(readOnly = true)
    public boolean exists(String email) {
        return memberRepository.existsByEmail(normalize(email));
    }

    /** 大文字・小文字や前後の空白の違いで別人扱いにならないようにそろえる */
    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
