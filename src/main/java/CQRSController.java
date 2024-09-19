

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

// Command (Write) Model

@Entity
@Table(name = "Posts")
 class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long postId;
    private Long userId;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Getters and setters
    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

@Entity
@Table(name = "Users")
class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;
    private String username;
    private String fullName;

    // Getters and setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
}

 class PostCreatedEvent extends DomainEvent {
    private final Long postId;
    private final Long userId;
    private final String content;
    private final LocalDateTime createdAt;

    public PostCreatedEvent(Long postId, Long userId, String content, LocalDateTime createdAt) {
        this.postId = postId;
        this.userId = userId;
        this.content = content;
        this.createdAt = createdAt;
    }

    // Getters
    public Long getPostId() { return postId; }
    public Long getUserId() { return userId; }
    public String getContent() { return content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

// Query (Read) Model

@Entity
@Table(name = "PostsReadModel")
class PostReadModel {
    @Id
    private Long postId;
    private Long userId;
    private String username;
    private String fullName;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long commentCount;
    private Long likeCount;

    // Getters and setters
    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Long getCommentCount() { return commentCount; }
    public void setCommentCount(Long commentCount) { this.commentCount = commentCount; }
    public Long getLikeCount() { return likeCount; }
    public void setLikeCount(Long likeCount) { this.likeCount = likeCount; }
}

@Repository
interface PostReadModelRepository extends JpaRepository<PostReadModel, Long> {
    List<PostReadModel> findTop10ByOrderByCreatedAtDesc();
}

@Repository
 interface UserRepository extends JpaRepository<User, Long> {
}

@Service
class PostQueryService {
    @Autowired
    private PostReadModelRepository postReadModelRepository;

    public PostReadModel getPost(Long postId) {
        return postReadModelRepository.findById(postId).orElse(null);
    }

    public List<PostReadModel> getRecentPosts() {
        return postReadModelRepository.findTop10ByOrderByCreatedAtDesc();
    }
}

@Service
 class PostReadModelUpdater {
    @Autowired
    private PostReadModelRepository postReadModelRepository;
    @Autowired
    private UserRepository userRepository;

    @EventListener
    @Transactional
    public void handlePostCreated(PostCreatedEvent event) {
        User user = userRepository.findById(event.getUserId()).orElseThrow();

        PostReadModel readModel = new PostReadModel();
        readModel.setPostId(event.getPostId());
        readModel.setUserId(event.getUserId());
        readModel.setUsername(user.getUsername());
        readModel.setFullName(user.getFullName());
        readModel.setContent(event.getContent());
        readModel.setCreatedAt(event.getCreatedAt());
        readModel.setUpdatedAt(event.getCreatedAt());
        readModel.setCommentCount(0L);
        readModel.setLikeCount(0L);

        postReadModelRepository.save(readModel);
    }

    // Other event handlers...
}

@Service
class PostCommandService {
    @Autowired
    private PostRepository postRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Transactional
    public Post createPost(Long userId, String content) {
        Post post = new Post();
        post.setUserId(userId);
        post.setContent(content);
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());

        post = postRepository.save(post);

        eventPublisher.publishEvent(new PostCreatedEvent(post.getPostId(), post.getUserId(), post.getContent(), post.getCreatedAt()));

        return post;
    }
}

@RestController
@RequestMapping("/posts")
 class PostController {
    @Autowired
    private PostCommandService postCommandService;

    @Autowired
    private PostQueryService postQueryService;

    @PostMapping
    public ResponseEntity<Post> createPost(@RequestBody CreatePostRequest request) {
        Post post = postCommandService.createPost(request.getUserId(), request.getContent());
        return ResponseEntity.ok(post);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostReadModel> getPost(@PathVariable Long postId) {
        PostReadModel post = postQueryService.getPost(postId);
        return post != null ? ResponseEntity.ok(post) : ResponseEntity.notFound().build();
    }

    @GetMapping("/recent")
    public ResponseEntity<List<PostReadModel>> getRecentPosts() {
        List<PostReadModel> posts = postQueryService.getRecentPosts();
        return ResponseEntity.ok(posts);
    }
}

// Additional classes needed

class CreatePostRequest {
    private Long userId;
    private String content;

    // Getters and setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}

@Repository
 interface PostRepository extends JpaRepository<Post, Long> {
}

 abstract class DomainEvent {
    // Add any common properties or methods for domain events
}