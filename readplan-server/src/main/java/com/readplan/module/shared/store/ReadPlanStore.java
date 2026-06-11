package com.readplan.module.shared.store;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.readplan.common.api.PageResult;
import com.readplan.common.exception.BusinessException;
import com.readplan.common.security.CurrentUser;
import com.readplan.module.shared.payload.ReadPlanPayloads.AdminImportCandidate;
import com.readplan.module.shared.payload.ReadPlanPayloads.BookDetail;
import com.readplan.module.shared.payload.ReadPlanPayloads.BookSummary;
import com.readplan.module.shared.payload.ReadPlanPayloads.CommentItem;
import com.readplan.module.shared.payload.ReadPlanPayloads.PublicNote;
import com.readplan.module.shared.payload.ReadPlanPayloads.ReadingPlanItem;
import com.readplan.module.shared.payload.ReadPlanPayloads.UserInfoResponse;
import com.readplan.module.shared.payload.ReadPlanPayloads.UserNoteSummary;
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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ReadPlanStore {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final UserMapper userMapper;
    private final BookMapper bookMapper;
    private final ReadingPlanMapper readingPlanMapper;
    private final NoteMapper noteMapper;
    private final CommentMapper commentMapper;
    private final ImportCandidateMapper importCandidateMapper;
    private final PasswordEncoder passwordEncoder;

    public ReadPlanStore(
        UserMapper userMapper,
        BookMapper bookMapper,
        ReadingPlanMapper readingPlanMapper,
        NoteMapper noteMapper,
        CommentMapper commentMapper,
        ImportCandidateMapper importCandidateMapper,
        PasswordEncoder passwordEncoder
    ) {
        this.userMapper = userMapper;
        this.bookMapper = bookMapper;
        this.readingPlanMapper = readingPlanMapper;
        this.noteMapper = noteMapper;
        this.commentMapper = commentMapper;
        this.importCandidateMapper = importCandidateMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserState createUser(String username, String rawPassword, String nickname, boolean admin) {
        return insertUser(username, passwordEncoder.encode(rawPassword), nickname, admin ? "ADMIN" : "USER");
    }

    @Transactional
    public UserState createUser(String username, String encodedPassword, String nickname, String role) {
        return insertUser(username, encodedPassword, nickname, role);
    }

    public UserState findUserByUsername(String username) {
        UserEntity entity = userMapper.selectOne(
            Wrappers.<UserEntity>lambdaQuery()
                .eq(UserEntity::getUsername, username)
                .eq(UserEntity::getDeleted, 0)
                .last("LIMIT 1")
        );
        return entity == null ? null : toUserState(entity);
    }

    public UserState findUserById(Long userId) {
        UserEntity entity = userMapper.selectById(userId);
        if (entity == null || isDeleted(entity.getDeleted())) {
            return null;
        }
        return toUserState(entity);
    }

    public CurrentUser toCurrentUser(UserState user) {
        return new CurrentUser(user.id(), user.username(), user.nickname(), List.of(user.role()), buildPermissions(user.role()));
    }

    public UserInfoResponse toUserInfo(CurrentUser currentUser) {
        return new UserInfoResponse(
            currentUser.id(),
            currentUser.username(),
            currentUser.nickname(),
            currentUser.roles(),
            currentUser.permissions()
        );
    }

    public PageResult<BookSummary> listBooks(String keyword, Integer pageNum, Integer pageSize) {
        int currentPage = Math.max(pageNum == null ? 1 : pageNum, 1);
        int size = Math.max(pageSize == null ? 10 : pageSize, 1);
        Page<BookEntity> page = bookMapper.selectPage(
            new Page<>(currentPage, size),
            Wrappers.<BookEntity>lambdaQuery()
                .eq(BookEntity::getDeleted, 0)
                .and(hasText(keyword), wrapper -> wrapper
                    .like(BookEntity::getTitle, keyword.trim())
                    .or()
                    .like(BookEntity::getAuthor, keyword.trim())
                    .or()
                    .like(BookEntity::getTags, keyword.trim())
                )
                .orderByAsc(BookEntity::getId)
        );
        return PageResult.of(
            page.getTotal(),
            page.getPages(),
            page.getRecords().stream().map(this::toBookSummary).toList()
        );
    }

    public BookDetail getBookDetail(Long bookId) {
        BookEntity book = requireBookIncludingDeleted(bookId);
        List<PublicNote> notes = getBookNotes(bookId);
        long planCount = readingPlanMapper.selectCount(
            Wrappers.<ReadingPlanEntity>lambdaQuery()
                .eq(ReadingPlanEntity::getBookId, bookId)
                .eq(ReadingPlanEntity::getDeleted, 0)
        );
        return new BookDetail(
            book.getId(),
            book.getTitle(),
            defaultString(book.getAuthor()),
            defaultString(book.getCover()),
            defaultNumber(book.getPublishYear()),
            defaultString(book.getDescription()),
            splitTags(book.getTags()),
            isTrue(book.getImported()),
            defaultString(book.getIsbn()),
            defaultString(book.getOlId()),
            notes.size(),
            (int) planCount,
            notes
        );
    }

    public List<BookSummary> listAdminBooks() {
        return bookMapper.selectList(
            Wrappers.<BookEntity>lambdaQuery()
                .eq(BookEntity::getDeleted, 0)
                .orderByAsc(BookEntity::getId)
        ).stream().map(this::toBookSummary).toList();
    }

    public List<AdminImportCandidate> searchImportCandidates(String keyword) {
        return importCandidateMapper.selectList(
            Wrappers.<ImportCandidateEntity>lambdaQuery()
                .and(hasText(keyword), wrapper -> wrapper
                    .like(ImportCandidateEntity::getTitle, keyword.trim())
                    .or()
                    .like(ImportCandidateEntity::getAuthor, keyword.trim())
                )
                .orderByAsc(ImportCandidateEntity::getOlId)
        ).stream().map(candidate -> new AdminImportCandidate(
            candidate.getOlId(),
            candidate.getTitle(),
            defaultString(candidate.getAuthor()),
            candidate.getFirstPublishYear(),
            defaultString(candidate.getCover()),
            false
        )).toList();
    }

    @Transactional
    public List<BookSummary> importBooks(List<String> olIds) {
        if (olIds == null || olIds.isEmpty()) {
            throw new BusinessException(400, "请先选择要导入的书籍");
        }

        List<BookSummary> importedBooks = new java.util.ArrayList<>();
        for (String olId : olIds) {
            ImportCandidateEntity candidate = importCandidateMapper.selectById(olId);
            if (candidate == null) {
                continue;
            }

            Long exists = bookMapper.selectCount(
                Wrappers.<BookEntity>lambdaQuery().eq(BookEntity::getOlId, candidate.getOlId())
            );
            if (exists != null && exists > 0) {
                continue;
            }

            BookEntity book = new BookEntity();
            LocalDateTime now = LocalDateTime.now();
            book.setTitle(candidate.getTitle());
            book.setAuthor(defaultString(candidate.getAuthor()));
            book.setCover(defaultString(candidate.getCover()));
            book.setPublishYear(defaultNumber(candidate.getFirstPublishYear()));
            book.setIsbn("");
            book.setOlId(candidate.getOlId());
            book.setDescription("来自 Open Library 检索结果，等待管理员补充 ISBN、简介等本地字段。");
            book.setTags("待完善");
            book.setImported(1);
            book.setDeleted(0);
            book.setCreatedAt(now);
            book.setUpdatedAt(now);
            bookMapper.insert(book);
            importedBooks.add(toBookSummary(book));
        }

        return importedBooks;
    }

    @Transactional
    public BookSummary createBook(
        String title,
        String author,
        String cover,
        Integer publishYear,
        String isbn,
        String olId,
        String description
    ) {
        LocalDateTime now = LocalDateTime.now();
        BookEntity book = new BookEntity();
        book.setTitle(title);
        book.setAuthor(defaultString(author));
        book.setCover(defaultString(cover));
        book.setPublishYear(defaultNumber(publishYear));
        book.setIsbn(defaultString(isbn));
        book.setOlId(defaultString(olId));
        book.setDescription(defaultString(description));
        book.setTags("手动录入");
        book.setImported(1);
        book.setDeleted(0);
        book.setCreatedAt(now);
        book.setUpdatedAt(now);
        bookMapper.insert(book);
        return toBookSummary(book);
    }

    @Transactional
    public BookSummary updateBook(
        Long id,
        String title,
        String author,
        String cover,
        Integer publishYear,
        String isbn,
        String olId,
        String description
    ) {
        BookEntity book = requireBook(id);
        book.setTitle(title);
        book.setAuthor(defaultString(author));
        book.setCover(defaultString(cover));
        book.setPublishYear(defaultNumber(publishYear));
        book.setIsbn(defaultString(isbn));
        book.setOlId(defaultString(olId));
        book.setDescription(defaultString(description));
        book.setUpdatedAt(LocalDateTime.now());
        bookMapper.updateById(book);
        return toBookSummary(book);
    }

    @Transactional
    public void softDeleteBook(Long id) {
        BookEntity book = requireBook(id);
        book.setDeleted(1);
        book.setUpdatedAt(LocalDateTime.now());
        bookMapper.updateById(book);
    }

    public List<ReadingPlanItem> getPlans(Long userId) {
        return readingPlanMapper.selectList(
            Wrappers.<ReadingPlanEntity>lambdaQuery()
                .eq(ReadingPlanEntity::getUserId, userId)
                .eq(ReadingPlanEntity::getDeleted, 0)
                .orderByDesc(ReadingPlanEntity::getUpdatedAt)
        ).stream().map(plan -> {
            BookEntity book = requireBookIncludingDeleted(plan.getBookId());
            boolean bookDeleted = isDeleted(book.getDeleted());
            return new ReadingPlanItem(
                plan.getId(),
                toBookSummary(book),
                defaultNumber(plan.getStatus()),
                bookDeleted ? "OFFLINE" : "ACTIVE",
                format(plan.getUpdatedAt()),
                defaultNumber(plan.getStatus()) != 0
            );
        }).toList();
    }

    @Transactional
    public ReadingPlanItem addPlan(Long userId, Long bookId, Integer status) {
        requireBookIncludingDeleted(bookId);
        Long exists = readingPlanMapper.selectCount(
            Wrappers.<ReadingPlanEntity>lambdaQuery()
                .eq(ReadingPlanEntity::getUserId, userId)
                .eq(ReadingPlanEntity::getBookId, bookId)
                .eq(ReadingPlanEntity::getDeleted, 0)
        );
        if (exists != null && exists > 0) {
            throw new BusinessException(400, "该书已经加入阅读计划");
        }

        LocalDateTime now = LocalDateTime.now();
        ReadingPlanEntity plan = new ReadingPlanEntity();
        plan.setUserId(userId);
        plan.setBookId(bookId);
        plan.setStatus(defaultNumber(status));
        plan.setDeleted(0);
        plan.setCreatedAt(now);
        plan.setUpdatedAt(now);
        readingPlanMapper.insert(plan);
        return toReadingPlanItem(plan, requireBookIncludingDeleted(bookId));
    }

    @Transactional
    public ReadingPlanItem updatePlanStatus(Long userId, Long planId, Integer status) {
        ReadingPlanEntity plan = readingPlanMapper.selectById(planId);
        if (plan == null || isDeleted(plan.getDeleted()) || !Objects.equals(plan.getUserId(), userId)) {
            throw new BusinessException(403, "只能修改自己的阅读计划");
        }
        plan.setStatus(defaultNumber(status));
        plan.setUpdatedAt(LocalDateTime.now());
        readingPlanMapper.updateById(plan);
        return toReadingPlanItem(plan, requireBookIncludingDeleted(plan.getBookId()));
    }

    @Transactional
    public void deletePlan(Long userId, Long planId) {
        ReadingPlanEntity plan = readingPlanMapper.selectById(planId);
        if (plan == null || isDeleted(plan.getDeleted()) || !Objects.equals(plan.getUserId(), userId)) {
            throw new BusinessException(403, "只能删除自己的阅读计划");
        }
        plan.setDeleted(1);
        plan.setUpdatedAt(LocalDateTime.now());
        readingPlanMapper.updateById(plan);
    }

    public List<UserNoteSummary> getUserNotes(Long userId) {
        return noteMapper.selectList(
            Wrappers.<NoteEntity>lambdaQuery()
                .eq(NoteEntity::getUserId, userId)
                .eq(NoteEntity::getDeleted, 0)
                .orderByDesc(NoteEntity::getCreatedAt)
        ).stream().map(note -> new UserNoteSummary(
            note.getId(),
            note.getBookId(),
            requireBookIncludingDeleted(note.getBookId()).getTitle(),
            defaultString(note.getTitle()),
            defaultString(note.getContent()),
            format(note.getCreatedAt()),
            countComments(note.getId())
        )).toList();
    }

    public List<PublicNote> getBookNotes(Long bookId) {
        return noteMapper.selectList(
            Wrappers.<NoteEntity>lambdaQuery()
                .eq(NoteEntity::getBookId, bookId)
                .eq(NoteEntity::getDeleted, 0)
                .orderByDesc(NoteEntity::getCreatedAt)
        ).stream().map(note -> new PublicNote(
            note.getId(),
            defaultString(note.getTitle()),
            requireUser(note.getUserId()).nickname(),
            defaultString(note.getContent()),
            format(note.getCreatedAt()),
            countComments(note.getId())
        )).toList();
    }

    @Transactional
    public UserNoteSummary createNote(Long userId, Long bookId, String title, String content) {
        ReadingPlanEntity plan = readingPlanMapper.selectOne(
            Wrappers.<ReadingPlanEntity>lambdaQuery()
                .eq(ReadingPlanEntity::getUserId, userId)
                .eq(ReadingPlanEntity::getBookId, bookId)
                .eq(ReadingPlanEntity::getDeleted, 0)
                .last("LIMIT 1")
        );
        if (plan == null || defaultNumber(plan.getStatus()) == 0) {
            throw new BusinessException(400, "只有加入阅读计划且开始阅读后才能写笔记");
        }

        LocalDateTime now = LocalDateTime.now();
        NoteEntity note = new NoteEntity();
        note.setUserId(userId);
        note.setBookId(bookId);
        note.setTitle(title);
        note.setContent(content);
        note.setDeleted(0);
        note.setCreatedAt(now);
        note.setUpdatedAt(now);
        noteMapper.insert(note);
        return toUserNoteSummary(note);
    }

    @Transactional
    public UserNoteSummary updateNote(Long userId, Long noteId, String title, String content) {
        NoteEntity note = noteMapper.selectById(noteId);
        if (note == null || isDeleted(note.getDeleted()) || !Objects.equals(note.getUserId(), userId)) {
            throw new BusinessException(403, "只能编辑自己的笔记");
        }
        note.setTitle(title);
        note.setContent(content);
        note.setUpdatedAt(LocalDateTime.now());
        noteMapper.updateById(note);
        return toUserNoteSummary(note);
    }

    @Transactional
    public void deleteNote(Long userId, Long noteId) {
        NoteEntity note = noteMapper.selectById(noteId);
        if (note == null || !Objects.equals(note.getUserId(), userId) || isDeleted(note.getDeleted())) {
            throw new BusinessException(403, "只能删除自己的笔记");
        }
        note.setDeleted(1);
        note.setUpdatedAt(LocalDateTime.now());
        noteMapper.updateById(note);
    }

    @Transactional
    public CommentItem createComment(Long userId, Long noteId, String content) {
        NoteEntity note = noteMapper.selectById(noteId);
        if (note == null || isDeleted(note.getDeleted())) {
            throw new BusinessException(400, "笔记不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        CommentEntity comment = new CommentEntity();
        comment.setUserId(userId);
        comment.setNoteId(noteId);
        comment.setContent(content);
        comment.setDeleted(0);
        comment.setCreatedAt(now);
        comment.setUpdatedAt(now);
        commentMapper.insert(comment);
        return new CommentItem(comment.getId(), requireUser(userId).nickname(), content, format(now), true);
    }

    public List<CommentItem> getComments(Long noteId, Long currentUserId) {
        return commentMapper.selectList(
            Wrappers.<CommentEntity>lambdaQuery()
                .eq(CommentEntity::getNoteId, noteId)
                .eq(CommentEntity::getDeleted, 0)
                .orderByDesc(CommentEntity::getCreatedAt)
        ).stream().map(comment -> new CommentItem(
            comment.getId(),
            requireUser(comment.getUserId()).nickname(),
            defaultString(comment.getContent()),
            format(comment.getCreatedAt()),
            canDeleteComment(currentUserId, comment)
        )).toList();
    }

    @Transactional
    public void deleteComment(Long currentUserId, Long commentId) {
        CommentEntity comment = commentMapper.selectById(commentId);
        if (comment == null || isDeleted(comment.getDeleted())) {
            throw new BusinessException(400, "评论不存在");
        }

        UserState currentUser = requireUser(currentUserId);
        NoteEntity note = noteMapper.selectById(comment.getNoteId());
        boolean canDelete = Objects.equals(comment.getUserId(), currentUserId)
            || (note != null && !isDeleted(note.getDeleted()) && Objects.equals(note.getUserId(), currentUserId))
            || "ADMIN".equals(currentUser.role());

        if (!canDelete) {
            throw new BusinessException(403, "无权限删除该评论");
        }

        comment.setDeleted(1);
        comment.setUpdatedAt(LocalDateTime.now());
        commentMapper.updateById(comment);
    }

    private boolean canDeleteComment(Long currentUserId, CommentEntity comment) {
        if (currentUserId == null) {
            return false;
        }

        UserState currentUser = requireUser(currentUserId);
        NoteEntity note = noteMapper.selectById(comment.getNoteId());
        return Objects.equals(comment.getUserId(), currentUserId)
            || (note != null && !isDeleted(note.getDeleted()) && Objects.equals(note.getUserId(), currentUserId))
            || "ADMIN".equals(currentUser.role());
    }

    private UserState insertUser(String username, String encodedPassword, String nickname, String role) {
        LocalDateTime now = LocalDateTime.now();
        UserEntity entity = new UserEntity();
        entity.setUsername(username);
        entity.setPassword(encodedPassword);
        entity.setNickname(nickname);
        entity.setAvatar("");
        entity.setRole(toRoleCode(role));
        entity.setDeleted(0);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        userMapper.insert(entity);
        return toUserState(entity);
    }

    private ReadingPlanItem toReadingPlanItem(ReadingPlanEntity plan, BookEntity book) {
        boolean bookDeleted = isDeleted(book.getDeleted());
        return new ReadingPlanItem(
            plan.getId(),
            toBookSummary(book),
            defaultNumber(plan.getStatus()),
            bookDeleted ? "OFFLINE" : "ACTIVE",
            format(plan.getUpdatedAt()),
            defaultNumber(plan.getStatus()) != 0
        );
    }

    private UserNoteSummary toUserNoteSummary(NoteEntity note) {
        return new UserNoteSummary(
            note.getId(),
            note.getBookId(),
            requireBookIncludingDeleted(note.getBookId()).getTitle(),
            defaultString(note.getTitle()),
            defaultString(note.getContent()),
            format(note.getCreatedAt()),
            countComments(note.getId())
        );
    }

    private BookSummary toBookSummary(BookEntity book) {
        return new BookSummary(
            book.getId(),
            book.getTitle(),
            defaultString(book.getAuthor()),
            defaultString(book.getCover()),
            defaultNumber(book.getPublishYear()),
            defaultString(book.getDescription()),
            splitTags(book.getTags()),
            isTrue(book.getImported()),
            defaultString(book.getIsbn()),
            defaultString(book.getOlId())
        );
    }

    private UserState toUserState(UserEntity entity) {
        return new UserState(
            entity.getId(),
            entity.getUsername(),
            entity.getPassword(),
            defaultString(entity.getNickname()),
            toRoleName(entity.getRole())
        );
    }

    private int countComments(Long noteId) {
        Long count = commentMapper.selectCount(
            Wrappers.<CommentEntity>lambdaQuery()
                .eq(CommentEntity::getNoteId, noteId)
                .eq(CommentEntity::getDeleted, 0)
        );
        return count == null ? 0 : count.intValue();
    }

    private UserState requireUser(Long userId) {
        UserState user = findUserById(userId);
        if (user == null) {
            throw new BusinessException(400, "用户不存在");
        }
        return user;
    }

    private BookEntity requireBook(Long bookId) {
        BookEntity book = bookMapper.selectById(bookId);
        if (book == null || isDeleted(book.getDeleted())) {
            throw new BusinessException(400, "书籍不存在");
        }
        return book;
    }

    private BookEntity requireBookIncludingDeleted(Long bookId) {
        BookEntity book = bookMapper.selectById(bookId);
        if (book == null) {
            throw new BusinessException(400, "书籍不存在");
        }
        return book;
    }

    private List<String> buildPermissions(String role) {
        if ("ADMIN".equals(role)) {
            return List.of("note:write", "comment:moderate", "book:manage");
        }
        return List.of("note:write");
    }

    private List<String> splitTags(String tags) {
        if (!hasText(tags)) {
            return List.of();
        }
        return Arrays.stream(tags.split(","))
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .toList();
    }

    private String format(LocalDateTime value) {
        return value == null ? "" : value.format(FORMATTER);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean isDeleted(Integer deleted) {
        return Objects.equals(deleted, 1);
    }

    private boolean isTrue(Integer value) {
        return Objects.equals(value, 1);
    }

    private int defaultNumber(Integer value) {
        return value == null ? 0 : value;
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private Integer toRoleCode(String role) {
        return "ADMIN".equalsIgnoreCase(role) ? 1 : 0;
    }

    private String toRoleName(Integer role) {
        return Objects.equals(role, 1) ? "ADMIN" : "USER";
    }

    public record UserState(Long id, String username, String password, String nickname, String role) {
    }
}
