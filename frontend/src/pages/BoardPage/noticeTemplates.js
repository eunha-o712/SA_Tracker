export const AUTH_NOTICE_TITLE = '[필독] 회원 인증 및 인증마크 안내'

export const AUTH_NOTICE_HTML = `<section class="notice-guide">
  <header class="notice-guide__hero">
    <span class="notice-guide__eyebrow">MEMBER VERIFICATION</span>
    <h2>회원 인증, 어렵지 않아요.</h2>
    <p>SA-TRACKER의 인증 단계와 인증마크 의미를 한눈에 확인해 주세요.</p>
  </header>

  <section class="notice-guide__block">
    <div class="notice-guide__heading">
      <span class="notice-guide__number">01</span>
      <div><small>EMAIL VERIFICATION</small><h3>먼저 이메일 인증을 완료해 주세요.</h3></div>
    </div>
    <p>회원가입 후 받은 인증메일의 버튼을 누르면 로그인이 가능합니다. 메일이 보이지 않으면 스팸함을 확인하고, 잠시 뒤 다시 시도해 주세요.</p>
  </section>

  <section class="notice-guide__block">
    <div class="notice-guide__heading">
      <span class="notice-guide__number">02</span>
      <div><small>VERIFICATION MARK</small><h3>하트 색상은 인증 단계를 뜻합니다.</h3></div>
    </div>
    <div class="notice-guide__badge-grid">
      <article class="notice-guide__badge-card is-orange">
        <span class="notice-guide__heart notice-guide__heart--orange" aria-label="주황색 하트"></span>
        <div><strong>주황 하트</strong><p>마이페이지에서 서든어택 계정의 OUID 연결을 완료한 회원입니다.</p></div>
      </article>
      <article class="notice-guide__badge-card is-green">
        <span class="notice-guide__heart notice-guide__heart--green" aria-label="초록색 하트"></span>
        <div><strong>초록 하트</strong><p>운영자가 제출된 증빙을 확인해 본인 인증을 완료한 회원입니다.</p></div>
      </article>
    </div>
  </section>

  <section class="notice-guide__block">
    <div class="notice-guide__heading">
      <span class="notice-guide__number">03</span>
      <div><small>OPERATOR CHECK</small><h3>초록 하트 전환은 이렇게 요청해 주세요.</h3></div>
    </div>
    <ol class="notice-guide__steps">
      <li><span>STEP 1</span><div><strong>서든어택 홈페이지 로그인</strong><p>인증할 닉네임의 넥슨 계정으로 서든어택 공식 홈페이지에 로그인해 주세요.</p></div></li>
      <li><span>STEP 2</span><div><strong>내 SA 명함 열기</strong><p>로그인된 계정의 닉네임과 SA 명함이 한 화면에 보이도록 준비해 주세요.</p></div></li>
      <li><span>STEP 3</span><div><strong>화면 캡처 후 문의에 첨부</strong><p>문의사항에 제목을 ‘[인증 요청] 서든어택 닉네임’으로 작성하고 캡처 이미지를 첨부해 주세요.</p></div></li>
      <li><span>STEP 4</span><div><strong>운영자 확인</strong><p>증빙 확인이 끝나면 주황 하트가 초록 하트로 변경됩니다.</p></div></li>
    </ol>
    <section class="notice-guide__warning">
      <strong>캡처 전 꼭 확인해 주세요.</strong>
      <p>비밀번호, 이메일, 실명, PC 정보 등 인증에 필요하지 않은 개인정보는 반드시 가려 주세요.</p>
    </section>
  </section>

  <section class="notice-guide__block notice-guide__block--last">
    <div class="notice-guide__heading">
      <span class="notice-guide__number">04</span>
      <div><small>OUID DISPUTE</small><h3>이미 다른 회원에게 연결된 닉네임인가요?</h3></div>
    </div>
    <p>문의사항에서 ‘OUID 연결 분쟁’을 선택하고 같은 방식의 로그인·SA 명함 증빙을 첨부해 주세요. 운영자가 소유권을 확인한 뒤 기존 연결과 계정 상태를 처리합니다.</p>
  </section>
</section>`
