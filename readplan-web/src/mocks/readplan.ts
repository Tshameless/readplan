import type {
  AdminImportCandidate,
  BookDetail,
  BookSummary,
  DashboardStat,
  ReadingPlanItem,
  UserNoteSummary,
} from '@/types/readplan';

export const dashboardStats: DashboardStat[] = [
  {
    label: '已入库书籍',
    value: '128',
    hint: '支持前台浏览、后台导入和软删除。',
  },
  {
    label: '公开笔记',
    value: '356',
    hint: '所有读书笔记默认公开可见，可继续扩展评论流。',
  },
  {
    label: '活跃阅读计划',
    value: '74',
    hint: '阅读状态分为未开始、阅读中、已读完。',
  },
];

export const books: BookSummary[] = [
  {
    id: '1',
    title: 'Effective Java',
    author: 'Joshua Bloch',
    cover: 'https://covers.openlibrary.org/b/id/8231856-L.jpg',
    publishYear: 2018,
    description: '围绕 Java 语言设计、对象创建、泛型与并发的经典实践指南。',
    tags: ['Java', '后端', '经典'],
    imported: true,
  },
  {
    id: '2',
    title: 'Clean Code',
    author: 'Robert C. Martin',
    cover: 'https://covers.openlibrary.org/b/id/9610928-L.jpg',
    publishYear: 2008,
    description: '以代码可读性、命名、函数和边界设计为核心的软件工程入门书。',
    tags: ['工程', '重构'],
    imported: true,
  },
  {
    id: '3',
    title: 'Designing Data-Intensive Applications',
    author: 'Martin Kleppmann',
    cover: 'https://covers.openlibrary.org/b/id/9251996-L.jpg',
    publishYear: 2017,
    description: '面向分布式系统、存储引擎和数据一致性的系统设计读物。',
    tags: ['架构', '数据库'],
    imported: true,
  },
  {
    id: '4',
    title: 'The Pragmatic Programmer',
    author: 'Andrew Hunt / David Thomas',
    cover: 'https://covers.openlibrary.org/b/id/10476739-L.jpg',
    publishYear: 2019,
    description: '强调持续反馈、自动化和工程判断力的通用开发方法论。',
    tags: ['工程', '方法论'],
    imported: false,
  },
];

export const bookDetails: Record<string, BookDetail> = {
  '1': {
    ...books[0],
    isbn: '9780134685991',
    openLibraryId: 'OL25428908M',
    noteCount: 18,
    planCount: 42,
    notes: [
      {
        id: 'n-101',
        title: '为什么优先考虑静态工厂方法',
        authorName: '阅读者',
        contentPreview: '静态工厂方法不仅能表达语义，还能控制实例缓存与返回子类型。',
        createdAt: '2026-06-09 21:32:00',
        commentCount: 4,
      },
      {
        id: 'n-102',
        title: '避免创建不必要对象的实践',
        authorName: '阿青',
        contentPreview: '在热点路径上减少装箱和临时对象分配，对吞吐和 GC 都很关键。',
        createdAt: '2026-06-10 08:45:00',
        commentCount: 2,
      },
    ],
  },
  '2': {
    ...books[1],
    isbn: '9780132350884',
    openLibraryId: 'OL10909569M',
    noteCount: 11,
    planCount: 37,
    notes: [
      {
        id: 'n-201',
        title: '好命名降低维护成本',
        authorName: '张三',
        contentPreview: '命名是最廉价、收益最高的维护动作，尤其在多人协作项目里。',
        createdAt: '2026-06-07 14:26:00',
        commentCount: 5,
      },
    ],
  },
  '3': {
    ...books[2],
    isbn: '9781449373320',
    openLibraryId: 'OL26442158M',
    noteCount: 6,
    planCount: 23,
    notes: [
      {
        id: 'n-301',
        title: '日志、指标、追踪是不同维度',
        authorName: '架构控',
        contentPreview: '三者并不互相替代，系统观测能力要围绕问题定位链路来设计。',
        createdAt: '2026-06-05 20:12:00',
        commentCount: 3,
      },
    ],
  },
  '4': {
    ...books[3],
    isbn: '9780135957059',
    openLibraryId: 'OL28157137M',
    noteCount: 3,
    planCount: 16,
    notes: [],
  },
};

export const readingPlans: ReadingPlanItem[] = [
  {
    id: 'p-1',
    book: books[0],
    status: 2,
    shelfState: 'ACTIVE',
    updatedAt: '2026-06-10 09:00:00',
    allowNote: true,
  },
  {
    id: 'p-2',
    book: books[2],
    status: 1,
    shelfState: 'ACTIVE',
    updatedAt: '2026-06-11 10:30:00',
    allowNote: true,
  },
  {
    id: 'p-3',
    book: books[3],
    status: 0,
    shelfState: 'OFFLINE',
    updatedAt: '2026-06-08 19:20:00',
    allowNote: false,
  },
];

export const userNotes: UserNoteSummary[] = [
  {
    id: 'my-1',
    bookId: '1',
    bookTitle: 'Effective Java',
    title: '对象创建方式的取舍',
    excerpt: 'Builder 更适合参数多且含可选参数的对象，既避免 telescoping constructor，也保留可读性。',
    createdAt: '2026-06-10 21:10:00',
    commentCount: 4,
  },
  {
    id: 'my-2',
    bookId: '3',
    bookTitle: 'Designing Data-Intensive Applications',
    title: '复制与一致性的平衡',
    excerpt: '同一套数据模型，在复制拓扑变化后，吞吐和一致性承诺会同时变化。',
    createdAt: '2026-06-11 08:40:00',
    commentCount: 1,
  },
];

export const importCandidates: AdminImportCandidate[] = [
  {
    olId: 'OL82563W',
    title: 'Refactoring',
    author: 'Martin Fowler',
    firstPublishYear: 1999,
    cover: 'https://covers.openlibrary.org/b/id/11153256-L.jpg',
    selected: false,
  },
  {
    olId: 'OL45883W',
    title: 'Domain-Driven Design',
    author: 'Eric Evans',
    firstPublishYear: 2003,
    cover: 'https://covers.openlibrary.org/b/id/12615128-L.jpg',
    selected: false,
  },
];
