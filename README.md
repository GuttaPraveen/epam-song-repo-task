# Resilient Messaging Service with Spring Boot

This Spring Boot application demonstrates how to handle third-party API failures while ensuring database records are always saved. It addresses the common problem where SMS/WhatsApp API failures prevent database transactions from completing.

## Problem Statement

**Original Issue:**
```java
// Problematic code - if messaging fails, record is not saved
smsStrikerGateway.sendSms(user.getMobile(), formattedMessage, smsTemplate);
commonFunctionUtils.sendWhatsAppMsg(ticketDetailsRequest.getTicketNumber(), 
                                   ticketDetailsRequest.getSubject(), 
                                   user.getMobile(), loginUser, 
                                   ticketDetailsRequest.getUserAction());
// Database save happens here - fails if messaging fails
```

**Problems:**
- If SMS/WhatsApp APIs are unavailable, database records don't get saved
- No retry mechanism for failed messages
- Blocking operations affect user experience
- All-or-nothing approach loses data

## Solution Architecture

### 1. **Separation of Concerns**
- Database operations happen first (guaranteed consistency)
- Messaging happens asynchronously (non-blocking)
- Failed messages are queued for retry

### 2. **Key Components**

#### **@Retry with Spring Retry**
- Automatic retry with exponential backoff
- Circuit breaker pattern for failing services
- Configurable retry attempts and intervals

#### **@Async with Spring Async**
- Non-blocking message processing
- Parallel execution of SMS and WhatsApp
- Separate thread pool for messaging operations

#### **@Scheduled with Spring Scheduling**
- Background processing of failed messages
- Exponential backoff retry strategy
- Automatic cleanup of old messages

### 3. **Message Queue Pattern**
- Failed messages stored in database queue
- Retry with exponential backoff (1min, 2min, 4min, 8min...)
- Maximum retry attempts configurable
- Message status tracking (PENDING, PROCESSING, SENT, FAILED, EXPIRED)

## Quick Start

### 1. Configure Application Properties

Update `src/main/resources/application.yml`:

```yaml
messaging:
  sms:
    api-url: "https://your-sms-provider.com/api/send"
    api-key: "your-sms-api-key"
  whatsapp:
    api-url: "https://your-whatsapp-provider.com/api/send"
    api-key: "your-whatsapp-api-key"
```

### 2. Run the Application

```bash
mvn spring-boot:run
```

### 3. Test the API

**Create a ticket:**
```bash
curl -X POST http://localhost:8080/api/tickets/create \
  -H "Content-Type: application/json" \
  -d '{
    "ticketNumber": "TKT001",
    "subject": "Test Ticket",
    "userMobile": "+1234567890",
    "userAction": "CREATE",
    "smsMessage": "Your ticket has been created",
    "smsTemplate": "ticket_creation",
    "loginUser": "admin"
  }'
```

## Usage in Your Existing Code

### Replace This (Problematic):
```java
@Transactional
public void saveTicket(TicketDetailsRequest request) {
    // This fails if messaging fails
    smsStrikerGateway.sendSms(user.getMobile(), message, template);
    commonFunctionUtils.sendWhatsAppMsg(request.getTicketNumber(), 
                                       request.getSubject(), 
                                       user.getMobile(), 
                                       loginUser, 
                                       request.getUserAction());
    
    // Database save - won't happen if above fails
    ticketRepository.save(ticket);
}
```

### With This (Resilient):
```java
@Autowired
private TicketService ticketService;

@Transactional
public Ticket saveTicket(TicketDetailsRequest request) {
    User user = getCurrentUser();
    String formattedMessage = formatMessage(request);
    
    // Database save happens first, messaging is async
    return ticketService.saveTicketWithMessaging(request, user, formattedMessage);
}
```

## Key Features

### 1. **Guaranteed Database Consistency**
- Records are saved to database first
- Messaging failures don't affect data persistence
- Transactional integrity maintained

### 2. **Resilient Messaging**
- Immediate attempt for fast delivery
- Queue-based retry for failures
- Exponential backoff strategy
- Maximum retry limits

### 3. **Monitoring and Observability**
- Comprehensive logging at all levels
- Message queue statistics
- Health check endpoints
- Retry attempt tracking

### 4. **Configurable Retry Strategy**
```yaml
messaging:
  retry:
    max-attempts: 3
    initial-interval: 1000
    max-interval: 10000
    multiplier: 2.0
```

### 5. **Automatic Cleanup**
- Old messages cleaned up daily
- Configurable retention period
- Prevents database bloat

## API Endpoints

- `POST /api/tickets/create` - Create ticket with messaging
- `PUT /api/tickets/{ticketNumber}/status` - Update ticket status
- `GET /api/tickets/{ticketNumber}` - Get ticket details
- `GET /api/tickets/health` - Health check

## Database Schema

The application automatically creates these tables:

- `tickets` - Main ticket records
- `message_queue` - Failed messages for retry

## Configuration Options

```yaml
# Retry Configuration
messaging:
  retry:
    max-attempts: 3              # Maximum retry attempts
    initial-interval: 1000       # Initial delay (ms)
    max-interval: 10000         # Maximum delay (ms)
    multiplier: 2.0             # Backoff multiplier

# Scheduling Configuration
scheduling:
  retry-failed-messages:
    fixed-delay: 300000         # Process every 5 minutes
  cleanup-old-messages:
    cron: "0 0 2 * * ?"        # Daily at 2 AM
```

## Benefits

1. **Data Integrity**: Records are never lost due to messaging failures
2. **User Experience**: Fast response times (messaging happens in background)
3. **Reliability**: Automatic retry with intelligent backoff
4. **Scalability**: Async processing with configurable thread pools
5. **Monitoring**: Full visibility into message processing status
6. **Maintainability**: Clean separation of concerns

## Production Considerations

1. **Database Indexing**: Add indexes on message_queue status and retry columns
2. **Monitoring**: Set up alerts for high failed message counts
3. **Scaling**: Configure thread pool sizes based on load
4. **API Keys**: Use secure configuration management
5. **Rate Limiting**: Implement rate limiting for third-party APIs

## Testing

The application includes H2 in-memory database for testing. Access the H2 console at:
`http://localhost:8080/h2-console`

## License

This is a demonstration project showing resilient messaging patterns in Spring Boot.