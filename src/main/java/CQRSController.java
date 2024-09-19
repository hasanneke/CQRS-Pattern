// Command (Write) Model

import com.sun.tools.javac.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.annotation.Id;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "Posts")
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long postId;
    private Long userId;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Getters and setters
}

// User entity (for reference)
@Entity
@Table(name = "Users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;
    private String username;
    private String fullName;

    // Getters and setters
}

// Updated PostCreatedEvent
public class PostCreatedEvent extends DomainEvent {
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
}

// Updated Query (Read) Model

@Entity
@Table(name = "PostsReadModel")
public class PostReadModel {
    @Id
    private Long postId;
    private Long userId;
    private String username;
    private String fullName;  // Added full name
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long commentCount;
    private Long likeCount;

    // Getters and setters
}

@Repository
public interface PostReadModelRepository extends JpaRepository<PostReadModel, Long> {
    List<PostReadModel> findTop10ByOrderByCreatedAtDesc();
}

@Service
public class PostQueryService {
    @Autowired
    private PostReadModelRepository postReadModelRepository;

    public PostReadModel getPost(Long postId) {
        return postReadModelRepository.findById(postId).orElse(null);
    }

    public List<PostReadModel> getRecentPosts() {
        return postReadModelRepository.findTop10ByOrderByCreatedAtDesc();
    }
}

// Updated Event handler for updating read model

@Service
public class PostReadModelUpdater {
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
        readModel.setFullName(user.getFullName());  // Set full name
        readModel.setContent(event.getContent());
        readModel.setCreatedAt(event.getCreatedAt());
        readModel.setUpdatedAt(event.getCreatedAt());
        readModel.setCommentCount(0L);
        readModel.setLikeCount(0L);

        postReadModelRepository.save(readModel);
    }

    // Other event handlers...
}

@RestController
@RequestMapping("/posts")
public class PostController {
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