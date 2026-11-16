-- 초기 데이터. 분류·콘텐츠를 더 넣을 때는 이 파일을 고치지 말고
-- 같은 형식의 INSERT 를 DB에 실행하거나 V3__... 파일을 새로 만든다. (QC-011)

-- 대분류 (QC-012). 자료구조만 active, 나머지는 '준비 중'.
INSERT INTO category (id, name, level, parent_id, description, sort_order, active) VALUES
    (1, '자료구조',     1, NULL, '데이터를 담고 꺼내는 방법',        1, TRUE),
    (2, '알고리즘',     1, NULL, '문제를 푸는 절차와 효율',          2, FALSE),
    (3, '운영체제',     1, NULL, '프로세스, 메모리, 파일 시스템',    3, FALSE),
    (4, '컴퓨터구조',   1, NULL, 'CPU와 메모리가 일하는 방식',       4, FALSE),
    (5, '네트워크',     1, NULL, '컴퓨터끼리 데이터를 주고받는 규칙', 5, FALSE),
    (6, '데이터베이스', 1, NULL, '데이터를 저장하고 조회하는 시스템', 6, FALSE);

-- 중분류 (자료구조)
INSERT INTO category (id, name, level, parent_id, description, sort_order, active) VALUES
    (7, '스택', 2, 1, '나중에 넣은 것이 먼저 나온다 (LIFO)', 1, TRUE),
    (8, '큐',   2, 1, '먼저 넣은 것이 먼저 나온다 (FIFO)',   2, TRUE);

-- 소분류
INSERT INTO category (id, name, level, parent_id, description, sort_order, active) VALUES
    (9,  'Push',    3, 7, '스택 맨 위에 데이터 넣기',   1, TRUE),
    (10, 'Pop',     3, 7, '스택 맨 위에서 데이터 꺼내기', 2, TRUE),
    (11, 'Enqueue', 3, 8, '큐 맨 뒤에 데이터 넣기',     1, TRUE),
    (12, 'Dequeue', 3, 8, '큐 맨 앞에서 데이터 꺼내기',  2, TRUE);

-- 학습 콘텐츠 (설명은 마크다운)
INSERT INTO cs_content (id, minor_category_id, title, explanation) VALUES
(1, 9, '스택 Push — 맨 위에 쌓기',
'## 한 줄 정의

**Push**는 스택의 맨 위(top)에 데이터를 하나 올리는 연산입니다.

스택은 접시를 쌓는 것과 같습니다. 새 접시는 항상 맨 위에 놓이고, 중간이나 바닥에 끼워 넣을 수 없습니다.

## 동작 순서

1. 스택이 가득 찼는지 확인합니다. (크기가 고정된 배열로 만든 경우)
2. `top` 을 한 칸 올립니다.
3. 그 자리에 새 데이터를 저장합니다.

애니메이션에서 10, 20, 30 을 차례로 넣으면 30 이 맨 위에 놓입니다. 가장 늦게 들어온 값이 가장 위에 있다는 점이 스택의 핵심입니다.

## 시간 복잡도

| 연산 | 시간 |
| --- | --- |
| Push | O(1) |

맨 위 위치만 알면 되므로 데이터가 몇 개든 걸리는 시간이 같습니다.

## 주의할 점

- **스택 오버플로**: 가득 찬 스택에 Push 하면 발생합니다. 함수 호출이 너무 깊어질 때 나는 StackOverflowError 가 바로 이 상황입니다.
- 크기가 자동으로 늘어나는 구현(동적 배열, 연결 리스트)에서는 가득 참 검사가 필요 없습니다.

## 코드로 보기

```java
Deque<Integer> stack = new ArrayDeque<>();
stack.push(10);
stack.push(20);
stack.push(30);   // 맨 위는 30
```
'),
(2, 10, '스택 Pop — 맨 위에서 꺼내기',
'## 한 줄 정의

**Pop**은 스택의 맨 위(top)에 있는 데이터를 꺼내서 돌려주는 연산입니다.

가장 나중에 넣은 데이터가 가장 먼저 나옵니다. 이 성질을 **LIFO**(Last In, First Out, 후입선출)라고 부릅니다.

## 동작 순서

1. 스택이 비어 있는지 확인합니다.
2. `top` 위치의 데이터를 읽습니다.
3. `top` 을 한 칸 내립니다.
4. 읽어 둔 데이터를 돌려줍니다.

애니메이션에서 10, 20, 30 순서로 쌓인 스택은 30, 20, 10 순서로 나옵니다. 넣은 순서와 정확히 반대입니다.

## 시간 복잡도

| 연산 | 시간 |
| --- | --- |
| Pop | O(1) |
| Peek (꺼내지 않고 맨 위만 보기) | O(1) |

## 주의할 점

- **스택 언더플로**: 빈 스택에서 Pop 하면 발생합니다. 꺼내기 전에 비어 있는지 먼저 확인해야 합니다.
- 맨 위가 아닌 데이터는 바로 꺼낼 수 없습니다. 그 위의 데이터를 모두 Pop 해야 합니다.

## 어디에 쓰일까

- 실행 취소(Ctrl+Z): 가장 최근 작업부터 되돌립니다.
- 브라우저 뒤로 가기: 가장 최근에 본 페이지로 돌아갑니다.
- 함수 호출: 가장 나중에 호출된 함수가 가장 먼저 끝납니다.

## 코드로 보기

```java
Deque<Integer> stack = new ArrayDeque<>();
stack.push(10);
stack.push(20);
stack.push(30);

int a = stack.pop();   // 30
int b = stack.pop();   // 20
boolean empty = stack.isEmpty();   // false (10 이 남아 있음)
```
'),
(3, 11, '큐 Enqueue — 맨 뒤에 줄 서기',
'## 한 줄 정의

**Enqueue**는 큐의 맨 뒤(rear)에 데이터를 하나 추가하는 연산입니다.

큐는 매표소 앞의 줄과 같습니다. 새로 온 사람은 항상 줄의 맨 뒤에 섭니다.

## 동작 순서

1. 큐가 가득 찼는지 확인합니다. (크기가 고정된 배열로 만든 경우)
2. `rear` 가 가리키는 자리에 새 데이터를 저장합니다.
3. `rear` 를 한 칸 뒤로 옮깁니다.

애니메이션에서 10, 20, 30 을 차례로 넣으면 10 이 맨 앞(front), 30 이 맨 뒤(rear)에 놓입니다. 스택과 달리 넣는 쪽과 꺼내는 쪽이 서로 다릅니다.

## 시간 복잡도

| 연산 | 시간 |
| --- | --- |
| Enqueue | O(1) |

맨 뒤 위치만 알면 되므로 데이터 개수와 상관없이 일정합니다.

## 주의할 점

- **오버플로**: 가득 찬 큐에 Enqueue 하면 발생합니다.
- 배열로 큐를 만들면 앞쪽에서 꺼낸 자리가 빈 채로 남습니다. 이 낭비를 없애려고 배열의 끝과 처음을 이어 붙인 **원형 큐**를 사용합니다.

## 코드로 보기

```java
Queue<Integer> queue = new ArrayDeque<>();
queue.offer(10);
queue.offer(20);
queue.offer(30);   // 맨 앞은 10, 맨 뒤는 30
```
'),
(4, 12, '큐 Dequeue — 맨 앞에서 나가기',
'## 한 줄 정의

**Dequeue**는 큐의 맨 앞(front)에 있는 데이터를 꺼내서 돌려주는 연산입니다.

가장 먼저 넣은 데이터가 가장 먼저 나옵니다. 이 성질을 **FIFO**(First In, First Out, 선입선출)라고 부릅니다.

## 동작 순서

1. 큐가 비어 있는지 확인합니다.
2. `front` 위치의 데이터를 읽습니다.
3. `front` 를 한 칸 뒤로 옮깁니다.
4. 읽어 둔 데이터를 돌려줍니다.

애니메이션에서 10, 20, 30 순서로 들어온 큐는 10, 20, 30 순서 그대로 나옵니다. 스택의 Pop 과 비교하면 나오는 순서가 반대입니다.

## 시간 복잡도

| 연산 | 시간 |
| --- | --- |
| Dequeue | O(1) |
| Peek (꺼내지 않고 맨 앞만 보기) | O(1) |

배열의 맨 앞을 지우고 나머지를 한 칸씩 당기면 O(n)이 됩니다. 그래서 데이터를 옮기는 대신 `front` 위치만 바꿉니다.

## 주의할 점

- **언더플로**: 빈 큐에서 Dequeue 하면 발생합니다. 꺼내기 전에 비어 있는지 먼저 확인해야 합니다.

## 어디에 쓰일까

- 프린터 대기열: 먼저 요청한 문서부터 인쇄합니다.
- 운영체제의 작업 스케줄링: 먼저 도착한 작업부터 처리합니다.
- 너비 우선 탐색(BFS): 먼저 발견한 정점부터 방문합니다.

## 코드로 보기

```java
Queue<Integer> queue = new ArrayDeque<>();
queue.offer(10);
queue.offer(20);
queue.offer(30);

int a = queue.poll();   // 10
int b = queue.poll();   // 20
boolean empty = queue.isEmpty();   // false (30 이 남아 있음)
```
');

-- 영상(GIF)·대체 이미지 파일 위치
INSERT INTO video (content_id, video_url, fallback_image_url) VALUES
    (1, '/media/animations/stack-push.gif',    '/media/fallback/stack-push.png'),
    (2, '/media/animations/stack-pop.gif',     '/media/fallback/stack-pop.png'),
    (3, '/media/animations/queue-enqueue.gif', '/media/fallback/queue-enqueue.png'),
    (4, '/media/animations/queue-dequeue.gif', '/media/fallback/queue-dequeue.png');
