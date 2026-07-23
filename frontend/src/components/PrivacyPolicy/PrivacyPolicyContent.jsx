import './PrivacyPolicyContent.css'

const POLICY_SECTIONS = [
  {
    title: '1. 수집하는 정보',
    content: '회원 가입과 로그인 과정에서 이메일, 암호화된 비밀번호, 서든어택 닉네임과 계정 연결을 위한 OUID를 처리합니다. 비공개 문의를 작성하면 문의 내용과 이용자가 직접 첨부한 증빙 이미지를 함께 처리합니다. 실명, 전화번호, 생년월일은 필수로 수집하지 않습니다.',
  },
  {
    title: '2. 이용 목적',
    content: '회원 로그인, 닉네임 기준 전적 제공, 즐겨찾기와 클랜 기능 제공, OUID 중복 연결 분쟁 처리, 서비스 오류 대응과 보안 유지를 위해 정보를 이용합니다.',
  },
  {
    title: '3. 외부 서비스 이용',
    content: '전적 조회에는 NEXON Open API를 사용합니다. AI 전투 분석 기능을 요청하면 닉네임과 공개 전적 통계가 분석 서비스에 전달될 수 있으며, 비밀번호는 전달하지 않습니다.',
  },
  {
    title: '4. 보관과 삭제',
    content: '회원 정보는 서비스 이용 기간 동안 보관하며, 회원 탈퇴 또는 처리 목적 달성 시 지체 없이 삭제합니다. 관계 법령에 따라 보존이 필요한 정보는 해당 기간 동안 분리하여 보관합니다.',
  },
  {
    title: '5. 이용자의 권리와 문의',
    content: '이용자는 본인의 정보 열람·수정·삭제를 요청할 수 있습니다. 개인정보 관련 문의는 contact@satracker.com으로 보내주세요.',
  },
]

function PrivacyPolicyContent({ titleId = 'privacy-title' }) {
  return (
    <section className="privacy-panel" aria-labelledby={titleId}>
      <span className="privacy-eyebrow">PRIVACY POLICY</span>
      <h1 id={titleId}>개인정보처리방침</h1>
      <p className="privacy-lead">
        SA-TRACKER는 서비스에 필요한 최소한의 정보만 처리하고 안전하게 관리합니다.
      </p>
      <div className="privacy-date">시행일: 2026년 7월 24일</div>

      <div className="privacy-sections">
        {POLICY_SECTIONS.map((section) => (
          <article key={section.title}>
            <h2>{section.title}</h2>
            <p>{section.content}</p>
          </article>
        ))}
      </div>
    </section>
  )
}

export default PrivacyPolicyContent
