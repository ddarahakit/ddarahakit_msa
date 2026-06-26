// 멘토링 화면 공통 표시 유틸

/** 이름의 첫 글자(이니셜 아바타용) */
export const initialOf = (name) => (String(name || '?').trim().charAt(0) || '?').toUpperCase()

const AVATAR_COLORS = [
  '#14BCED', '#6366F1', '#F59E0B', '#10B981',
  '#EF4444', '#8B5CF6', '#EC4899', '#0EA5E9', '#F97316', '#22C55E',
]

/** idx/이름 기반 결정적 아바타 색상 */
export const avatarColor = (key) => {
  const s = String(key ?? '')
  let h = 0
  for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) >>> 0
  return AVATAR_COLORS[h % AVATAR_COLORS.length]
}

// 백엔드는 시각을 UTC 벽시계(타임존 표기 없음)로 내려준다 → UTC 로 파싱.
const toDate = (s) => {
  if (!s) return null
  const hasZone = /[zZ]|[+-]\d{2}:?\d{2}$/.test(s)
  const d = new Date(hasZone ? s : `${s}Z`)
  return isNaN(d.getTime()) ? null : d
}

/** 상대 시간 표기("방금 전" / "N분 전" / "N시간 전" / "N일 전" / "M월 D일") */
export const formatTimeAgo = (s) => {
  const d = toDate(s)
  if (!d) return ''
  const sec = Math.max(0, Math.floor((Date.now() - d.getTime()) / 1000))
  if (sec < 60) return '방금 전'
  const min = Math.floor(sec / 60)
  if (min < 60) return `${min}분 전`
  const hr = Math.floor(min / 60)
  if (hr < 24) return `${hr}시간 전`
  const day = Math.floor(hr / 24)
  if (day < 7) return `${day}일 전`
  return `${d.getMonth() + 1}월 ${d.getDate()}일`
}

/** 예약 일정 표기(YYYY.MM.DD HH:mm) */
export const formatSchedule = (s) => {
  const d = toDate(s)
  if (!d) return '미정'
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}.${p(d.getMonth() + 1)}.${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

/**
 * 로그인 사용자 기준 상대방/내 역할 계산.
 * 세션은 mentor/mentee 객체({idx,name,profileImageUrl}) 또는 평면 필드 모두 수용.
 */
export const getCounterpart = (session = {}, myIdx = null) => {
  const mentor = session.mentor || { name: session.mentorName, profileImageUrl: session.mentorProfileImageUrl }
  const mentee = session.mentee || { name: session.menteeName, profileImageUrl: session.menteeProfileImageUrl }
  const me = myIdx == null ? null : Number(myIdx)
  const iAmMentor = me != null && Number(mentor?.idx) === me

  return {
    other: iAmMentor ? mentee : mentor,
    otherRole: iAmMentor ? '멘티' : '멘토',
    myRole: iAmMentor ? '멘토' : '멘티',
    myReadAt: iAmMentor ? session.mentorReadAt : session.menteeReadAt,
  }
}

/** 현재 사용자에게 안 읽은 새 메시지가 있는지 */
export const hasUnread = (session = {}, myIdx = null) => {
  if (!session.lastMessageAt) return false
  const { myReadAt } = getCounterpart(session, myIdx)
  if (!myReadAt) return true
  return new Date(`${session.lastMessageAt}Z`) > new Date(`${myReadAt}Z`)
}
