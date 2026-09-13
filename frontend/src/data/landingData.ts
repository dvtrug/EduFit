export type MatchCard = {
  eyebrow: string
  title: string
  detail: string
  tone: 'lavender' | 'peach' | 'lime'
  progress: number
}

export const matchCards: MatchCard[] = [
  {
    eyebrow: '01 · GIA SƯ',
    title: '94% phù hợp',
    detail: 'Nguyễn Minh Anh · Toán 9',
    tone: 'lavender',
    progress: 94,
  },
  {
    eyebrow: '02 · LỊCH HỌC',
    title: 'Trùng 2 lịch',
    detail: 'Tối Thứ 3 và Thứ 6',
    tone: 'peach',
    progress: 72,
  },
  {
    eyebrow: '03 · MỤC TIÊU',
    title: 'Sẵn sàng bắt đầu',
    detail: 'Phương trình bậc hai',
    tone: 'lime',
    progress: 84,
  },
]

export const navigation = [
  { label: 'Cách hoạt động', href: '#cach-hoat-dong' },
  { label: 'Gia sư', href: '#gia-su' },
  { label: 'Tiến độ học', href: '#tien-do' },
  { label: 'Câu hỏi thường gặp', href: '#faq' },
]

export const trustItems = [
  {
    index: '01',
    label: 'Gia sư đã xác minh',
    detail: 'Hồ sơ chuyên môn và minh chứng được kiểm tra trước khi xuất hiện.',
  },
  {
    index: '02',
    label: 'Matching có giải thích',
    detail: 'Biết rõ vì sao một gia sư phù hợp với mục tiêu, lịch và ngân sách.',
  },
  {
    index: '03',
    label: 'Tiến độ có dữ liệu',
    detail: 'Milestone, quiz và báo cáo buổi học được tổng hợp tại một nơi.',
  },
]

export const matchingSteps = [
  {
    number: '01',
    title: 'Chia sẻ mục tiêu',
    detail: 'Cho EduFit biết môn học, lớp, mục tiêu, lịch rảnh và cách học phù hợp.',
    tone: 'blue',
  },
  {
    number: '02',
    title: 'Nhận gợi ý có lý do',
    detail: 'Nhận danh sách ngắn các gia sư đã xác minh cùng lý do phù hợp cụ thể.',
    tone: 'orange',
  },
  {
    number: '03',
    title: 'Chọn và bắt đầu học',
    detail: 'Xem hồ sơ, thống nhất lịch và bắt đầu một lộ trình có thể theo dõi.',
    tone: 'green',
  },
]

export const tutors = [
  {
    initials: 'MA',
    name: 'Nguyễn Minh Anh',
    subject: 'Toán THCS · Ôn thi lớp 10',
    score: '94% phù hợp',
    reasons: ['Mạnh Đại số', 'Dạy bằng sơ đồ', 'Trùng 2 lịch'],
    tone: 'blue',
  },
  {
    initials: 'HN',
    name: 'Trần Hoàng Nam',
    subject: 'Toán 8–9 · Củng cố nền tảng',
    score: '88% phù hợp',
    reasons: ['5 năm kinh nghiệm', 'Theo sát bài tập', 'Học online'],
    tone: 'orange',
  },
  {
    initials: 'TL',
    name: 'Lê Thanh Lam',
    subject: 'Ngữ văn THCS · Luyện viết',
    score: '85% phù hợp',
    reasons: ['Phản hồi chi tiết', 'Hợp mục tiêu', 'Trùng ngân sách'],
    tone: 'green',
  },
]

export const faqItems = [
  {
    question: 'EduFit xác minh gia sư như thế nào?',
    answer:
      'EduFit kiểm tra hồ sơ chuyên môn và tài liệu minh chứng trước khi gia sư được hiển thị trong kết quả tìm kiếm và matching.',
  },
  {
    question: 'Vì sao một gia sư được gợi ý cho tôi?',
    answer:
      'Gợi ý dựa trên môn, lớp, mục tiêu, lịch rảnh, hình thức học, khu vực và ngân sách. Mỗi kết quả đều đi kèm lý do phù hợp.',
  },
  {
    question: 'Phụ huynh theo dõi tiến độ bằng cách nào?',
    answer:
      'Phụ huynh đã liên kết có thể xem lịch học, milestone, kết quả quiz và báo cáo được gia sư xác nhận.',
  },
  {
    question: 'Tôi có thể đổi gia sư không?',
    answer:
      'Có. Bạn có thể xem lại các gợi ý khác và gửi yêu cầu mới nếu lựa chọn hiện tại không còn phù hợp.',
  },
]
