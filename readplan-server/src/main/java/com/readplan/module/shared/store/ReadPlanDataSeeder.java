package com.readplan.module.shared.store;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.readplan.module.shared.store.entity.BookEntity;
import com.readplan.module.shared.store.entity.CommentEntity;
import com.readplan.module.shared.store.entity.ImportCandidateEntity;
import com.readplan.module.shared.store.entity.NoteEntity;
import com.readplan.module.shared.store.entity.ReadingPlanEntity;
import com.readplan.module.shared.store.entity.UserEntity;
import com.readplan.module.shared.store.mapper.BookMapper;
import com.readplan.module.shared.store.mapper.CommentMapper;
import com.readplan.module.shared.store.mapper.ImportCandidateMapper;
import com.readplan.module.shared.store.mapper.NoteMapper;
import com.readplan.module.shared.store.mapper.ReadingPlanMapper;
import com.readplan.module.shared.store.mapper.UserMapper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ReadPlanDataSeeder implements ApplicationRunner {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final UserMapper userMapper;
    private final BookMapper bookMapper;
    private final ReadingPlanMapper readingPlanMapper;
    private final NoteMapper noteMapper;
    private final CommentMapper commentMapper;
    private final ImportCandidateMapper importCandidateMapper;
    private final JdbcTemplate jdbcTemplate;
    private final BookPdfCrawlerService bookPdfCrawlerService;
    private final PasswordEncoder passwordEncoder;
    private final Path bookStorageDir;

    public ReadPlanDataSeeder(
        UserMapper userMapper,
        BookMapper bookMapper,
        ReadingPlanMapper readingPlanMapper,
        NoteMapper noteMapper,
        CommentMapper commentMapper,
        ImportCandidateMapper importCandidateMapper,
        JdbcTemplate jdbcTemplate,
        BookPdfCrawlerService bookPdfCrawlerService,
        PasswordEncoder passwordEncoder,
        @Value("${readplan.storage.book-dir:./storage/books}") String bookStorageDir
    ) {
        this.userMapper = userMapper;
        this.bookMapper = bookMapper;
        this.readingPlanMapper = readingPlanMapper;
        this.noteMapper = noteMapper;
        this.commentMapper = commentMapper;
        this.importCandidateMapper = importCandidateMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.bookPdfCrawlerService = bookPdfCrawlerService;
        this.passwordEncoder = passwordEncoder;
        this.bookStorageDir = Path.of(bookStorageDir).toAbsolutePath().normalize();
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        upgradeBookSchema();
        upgradeImportCandidateSchema();

        Long userCount = userMapper.selectCount(Wrappers.<UserEntity>lambdaQuery().eq(UserEntity::getDeleted, 0));
        if (userCount != null && userCount > 0) {
            repairSeedState();
            return;
        }

        UserEntity admin = createUser("admin", "123456", "系统管理员", 1);
        UserEntity reader = createUser("reader", "123456", "阅读者", 0);
        UserEntity zhangsan = createUser("zhangsan", "123456", "张三", 0);
        UserEntity aQing = createUser("aqing", "123456", "阿青", 0);
        UserEntity architect = createUser("architect", "123456", "架构控", 0);

        BookEntity effectiveJava = createBook(
            "Effective Java",
            "Joshua Bloch",
            "https://covers.openlibrary.org/b/id/8231856-L.jpg",
            2018,
            "9780134685991",
            "OL25428908M",
            "围绕 Java 语言设计、对象创建、泛型与并发的经典实践指南。",
            "Java,后端,经典",
            1
        );
        BookEntity cleanCode = createBook(
            "Clean Code",
            "Robert C. Martin",
            "https://covers.openlibrary.org/b/id/9610928-L.jpg",
            2008,
            "9780132350884",
            "OL10909569M",
            "以代码可读性、命名、函数和边界设计为核心的软件工程入门书。",
            "工程,重构",
            1
        );
        BookEntity ddia = createBook(
            "Designing Data-Intensive Applications",
            "Martin Kleppmann",
            "https://covers.openlibrary.org/b/id/9251996-L.jpg",
            2017,
            "9781449373320",
            "OL26442158M",
            "面向分布式系统、存储引擎和数据一致性的系统设计读物。",
            "架构,数据库",
            1
        );
        BookEntity pragmatic = createBook(
            "The Pragmatic Programmer",
            "Andrew Hunt / David Thomas",
            "https://covers.openlibrary.org/b/id/10476739-L.jpg",
            2019,
            "9780135957059",
            "OL28157137M",
            "强调持续反馈、自动化和工程判断力的通用开发方法论。",
            "工程,方法论",
            0
        );
        pragmatic.setDeleted(1);
        pragmatic.setUpdatedAt(LocalDateTime.now());
        bookMapper.updateById(pragmatic);

        createPlan(reader.getId(), effectiveJava.getId(), 2, "2026-06-10 09:00:00");
        createPlan(reader.getId(), ddia.getId(), 1, "2026-06-11 10:30:00");
        createPlan(reader.getId(), pragmatic.getId(), 0, "2026-06-08 19:20:00");
        createPlan(zhangsan.getId(), cleanCode.getId(), 1, "2026-06-09 11:22:00");
        createPlan(admin.getId(), effectiveJava.getId(), 1, "2026-06-07 16:00:00");

        NoteEntity note1 = createNote(
            reader.getId(),
            effectiveJava.getId(),
            "为什么优先考虑静态工厂方法",
            "静态工厂方法不仅能表达语义，还能控制实例缓存与返回子类型。",
            "2026-06-09 21:32:00"
        );
        NoteEntity note2 = createNote(
            aQing.getId(),
            effectiveJava.getId(),
            "避免创建不必要对象的实践",
            "在热点路径上减少装箱和临时对象分配，对吞吐和 GC 都很关键。",
            "2026-06-10 08:45:00"
        );
        NoteEntity note3 = createNote(
            zhangsan.getId(),
            cleanCode.getId(),
            "好命名降低维护成本",
            "命名是最廉价、收益最高的维护动作，尤其在多人协作项目里。",
            "2026-06-07 14:26:00"
        );
        NoteEntity note4 = createNote(
            architect.getId(),
            ddia.getId(),
            "日志、指标、追踪是不同维度",
            "三者并不互相替代，系统观测能力要围绕问题定位链路来设计。",
            "2026-06-05 20:12:00"
        );
        createNote(
            reader.getId(),
            effectiveJava.getId(),
            "对象创建方式的取舍",
            "Builder 更适合参数多且含可选参数的对象，既避免 telescoping constructor，也保留可读性。",
            "2026-06-10 21:10:00"
        );
        createNote(
            reader.getId(),
            ddia.getId(),
            "复制与一致性的平衡",
            "同一套数据模型，在复制拓扑变化后，吞吐和一致性承诺会同时变化。",
            "2026-06-11 08:40:00"
        );

        createComment(zhangsan.getId(), note1.getId(), "你的笔记写得太好了，深有同感！", "2026-06-10 10:00:00");
        createComment(admin.getId(), note1.getId(), "这部分非常适合继续展开成对象创建章节总结。", "2026-06-10 10:15:00");
        createComment(reader.getId(), note3.getId(), "命名这块确实是最容易被低估的基本功。", "2026-06-08 09:20:00");

        createImportCandidate(
            "OL82563W",
            "Refactoring",
            "Martin Fowler",
            1999,
            "https://covers.openlibrary.org/b/id/11153256-L.jpg",
            "9780201485677",
            "重构经典书籍候选数据。",
            "重构,经典"
        );
        createImportCandidate(
            "OL45883W",
            "Domain-Driven Design",
            "Eric Evans",
            2003,
            "https://covers.openlibrary.org/b/id/12615128-L.jpg",
            "9780321125217",
            "领域驱动设计候选数据。",
            "架构,DDD"
        );

        autoSeedPdfUrls();
    }

    private void repairSeedState() {
        BookEntity pragmatic = bookMapper.selectOne(
            Wrappers.<BookEntity>lambdaQuery()
                .eq(BookEntity::getOlId, "OL28157137M")
                .last("LIMIT 1")
        );
        if (pragmatic != null && !Integer.valueOf(1).equals(pragmatic.getDeleted())) {
            pragmatic.setDeleted(1);
            pragmatic.setUpdatedAt(LocalDateTime.now());
            bookMapper.updateById(pragmatic);
        }

        ensureImportCandidate(
            "OL82563W",
            "Refactoring",
            "Martin Fowler",
            1999,
            "https://covers.openlibrary.org/b/id/11153256-L.jpg",
            "9780201485677",
            "重构经典书籍候选数据。",
            "重构,经典"
        );
        ensureImportCandidate(
            "OL45883W",
            "Domain-Driven Design",
            "Eric Evans",
            2003,
            "https://covers.openlibrary.org/b/id/12615128-L.jpg",
            "9780321125217",
            "领域驱动设计候选数据。",
            "架构,DDD"
        );

        autoSeedPdfUrls();
    }

    private void autoSeedPdfUrls() {
        try {
            // Clean up any old blocked raw.githubusercontent.com file paths
            jdbcTemplate.execute("UPDATE book SET file_path = '', file_type = '' WHERE file_path LIKE '%raw.githubusercontent.com%'");

            // Auto-trigger background crawl for seeded books
            triggerAutoCrawl(1L);
            triggerAutoCrawl(2L);
            triggerAutoCrawl(3L);
        } catch (Exception e) {
            System.err.println("Auto-seeding / crawling demo PDF URLs failed: " + e.getMessage());
        }
    }

    private void triggerAutoCrawl(Long bookId) {
        BookEntity book = bookMapper.selectById(bookId);
        if (book != null && (book.getFilePath() == null || book.getFilePath().isBlank())) {
            System.out.println("Auto-triggering background crawl for book: " + book.getTitle());
            bookPdfCrawlerService.crawlAndDownloadPdfAsync(bookId);
        }
    }

    private UserEntity createUser(String username, String rawPassword, String nickname, Integer role) {
        LocalDateTime now = LocalDateTime.now();
        UserEntity entity = new UserEntity();
        entity.setUsername(username);
        entity.setPassword(passwordEncoder.encode(rawPassword));
        entity.setNickname(nickname);
        entity.setRole(role);
        entity.setDeleted(0);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        userMapper.insert(entity);
        return entity;
    }

    private BookEntity createBook(
        String title,
        String author,
        String cover,
        Integer publishYear,
        String isbn,
        String olId,
        String description,
        String tags,
        Integer imported
    ) {
        LocalDateTime now = LocalDateTime.now();
        BookEntity entity = new BookEntity();
        entity.setTitle(title);
        entity.setAuthor(author);
        entity.setCover(cover);
        entity.setPublishYear(publishYear);
        entity.setIsbn(isbn);
        entity.setOlId(olId);
        entity.setDescription(description);
        entity.setTags(tags);
        entity.setFilePath("");
        entity.setFileType("");
        entity.setImported(imported);
        entity.setDeleted(0);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        bookMapper.insert(entity);
        return entity;
    }

    private void createPlan(Long userId, Long bookId, Integer status, String updatedAt) {
        ReadingPlanEntity entity = new ReadingPlanEntity();
        entity.setUserId(userId);
        entity.setBookId(bookId);
        entity.setStatus(status);
        entity.setDeleted(0);
        entity.setCreatedAt(parse(updatedAt));
        entity.setUpdatedAt(parse(updatedAt));
        readingPlanMapper.insert(entity);
    }

    private NoteEntity createNote(Long userId, Long bookId, String title, String content, String createdAt) {
        NoteEntity entity = new NoteEntity();
        entity.setUserId(userId);
        entity.setBookId(bookId);
        entity.setTitle(title);
        entity.setContent(content);
        entity.setDeleted(0);
        entity.setCreatedAt(parse(createdAt));
        entity.setUpdatedAt(parse(createdAt));
        noteMapper.insert(entity);
        return entity;
    }

    private void createComment(Long userId, Long noteId, String content, String createdAt) {
        CommentEntity entity = new CommentEntity();
        entity.setUserId(userId);
        entity.setNoteId(noteId);
        entity.setContent(content);
        entity.setDeleted(0);
        entity.setCreatedAt(parse(createdAt));
        entity.setUpdatedAt(parse(createdAt));
        commentMapper.insert(entity);
    }

    private void createImportCandidate(
        String olId,
        String title,
        String author,
        Integer firstPublishYear,
        String cover,
        String isbn,
        String description,
        String tags
    ) {
        ImportCandidateEntity entity = new ImportCandidateEntity();
        entity.setOlId(olId);
        entity.setTitle(title);
        entity.setAuthor(author);
        entity.setFirstPublishYear(firstPublishYear);
        entity.setCover(cover);
        entity.setIsbn(isbn);
        entity.setDescription(description);
        entity.setTags(tags);
        importCandidateMapper.insert(entity);
    }

    private void ensureImportCandidate(
        String olId,
        String title,
        String author,
        Integer firstPublishYear,
        String cover,
        String isbn,
        String description,
        String tags
    ) {
        ImportCandidateEntity entity = importCandidateMapper.selectById(olId);
        if (entity != null) {
            boolean changed = false;
            if (!isbn.equals(entity.getIsbn())) {
                entity.setIsbn(isbn);
                changed = true;
            }
            if (!description.equals(entity.getDescription())) {
                entity.setDescription(description);
                changed = true;
            }
            if (!tags.equals(entity.getTags())) {
                entity.setTags(tags);
                changed = true;
            }
            if (changed) {
                importCandidateMapper.updateById(entity);
            }
            return;
        }
        createImportCandidate(olId, title, author, firstPublishYear, cover, isbn, description, tags);
    }

    private void upgradeImportCandidateSchema() {
        ensureImportCandidateColumn("isbn", "ALTER TABLE import_candidate ADD COLUMN isbn VARCHAR(20)");
        ensureImportCandidateColumn("description", "ALTER TABLE import_candidate ADD COLUMN description TEXT");
        ensureImportCandidateColumn("tags", "ALTER TABLE import_candidate ADD COLUMN tags VARCHAR(255)");
    }

    private void upgradeBookSchema() {
        ensureTableColumn("book", "file_path", "ALTER TABLE book ADD COLUMN file_path VARCHAR(500)");
        ensureTableColumn("book", "file_type", "ALTER TABLE book ADD COLUMN file_type VARCHAR(20)");
    }

    private void ensureImportCandidateColumn(String columnName, String ddl) {
        ensureTableColumn("import_candidate", columnName, ddl);
    }

    private void ensureTableColumn(String tableName, String columnName, String ddl) {
        Integer count = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE LOWER(table_name) = ?
                  AND LOWER(column_name) = ?
                """,
            Integer.class,
            tableName.toLowerCase(),
            columnName.toLowerCase()
        );
        if (count == null || count == 0) {
            jdbcTemplate.execute(ddl);
        }
    }

    private LocalDateTime parse(String value) {
        return LocalDateTime.parse(value, FORMATTER);
    }
}
