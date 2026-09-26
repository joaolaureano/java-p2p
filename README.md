<a id="top"></a>
<h1 align="center">java-p2p</h1>

<p align="center">A UDP peer-to-peer network where a ring of super-nodes partitions the MD5 key space — a small DHT written from scratch in Java 21, with zero runtime dependencies.</p>

<p align="center">
  <a href="https://github.com/joaolaureano/java-p2p/actions/workflows/ci.yml"><img alt="CI" src="https://github.com/joaolaureano/java-p2p/actions/workflows/ci.yml/badge.svg"></a>
  <img alt="Java 21" src="https://img.shields.io/badge/Java-21-orange">
  <img alt="Maven" src="https://img.shields.io/badge/build-Maven-blue">
  <img alt="JUnit 5" src="https://img.shields.io/badge/tests-JUnit%205-green">
  <img alt="MIT License" src="https://img.shields.io/badge/license-MIT-yellow">
</p>

<p align="center"><b>🇺🇸 English</b> · <a href="README.pt-BR.md">🇧🇷 Português</a></p>

---

## Contents

- [Overview](#en-overview)
- [Architecture](#en-architecture)
- [How the DHT works](#en-dht)
- [Protocol](#en-protocol)
- [Project structure](#en-structure)
- [Getting started](#en-getting-started)
- [Tests](#en-tests)
- [What changed since v1](#en-changes)
- [Known limitations](#en-limitations)
- [Academic context](#en-academic)
- [License](#en-license)

<a id="en-overview"></a>
## Overview

java-p2p is a peer-to-peer network built on UDP. A ring of super-nodes splits the MD5 key space (2^128) into equal slices — a simple distributed hash table.

Each peer registers with one super-node (`create <nickname>`), stays alive with heartbeats every 5 s (it expires after 15 s of silence), publishes the MD5 hash of the content it shares (`register`), can list every resource in the ring (`list`), and asks another peer directly for the content (`resource <hash> <host> <port>`). Requests that the current super-node does not own travel around the ring (`register_ring`, `list_ring`) carrying their origin, and stop when they get back to it.

[↑ back to top](#top)

<a id="en-architecture"></a>
## Architecture

```mermaid
flowchart LR
    SN1["super-node 1 :9000"] --> SN2["super-node 2 :9001"]
    SN2 --> SN3["super-node 3 :9002"]
    SN3 --> SN1
    alice(["peer alice"]) --> SN1
    bob(["peer bob"]) --> SN3
    bob -. "resource hash" .-> alice
```

Three super-nodes form the ring. Each one owns a slice of the hash space, and each peer attaches to a single super-node for `create`, `heartbeat`, `register` and `list`. The content itself never goes through the ring: a peer asks the peer that holds it directly, with `resource <hash>`.

[↑ back to top](#top)

<a id="en-dht"></a>
## How the DHT works

Every resource is identified by the MD5 hash of its content. The ring divides the 2^128 key space into `N` equal slices:

```
slice = 2^128 / N
node i owns [slice * (i - 1), slice * i - 1]
the last node owns up to 2^128 - 1
```

Giving the remainder to the last node means no hash is ever left without an owner, even when `N` does not divide 2^128 exactly.

For a ring of 3 nodes:

| Node | Port | Hash range |
| --- | --- | --- |
| super-node 1 | 9000 | `0000…0` – `5555…4` |
| super-node 2 | 9001 | `5555…5` – `aaaa…9` |
| super-node 3 | 9002 | `aaaa…a` – `ffff…f` |

Each super-node logs the range it owns when it starts:

```
[node 9000] Owns HashRange[00000000000000000000000000000000 .. 55555555555555555555555555555554]
```

[↑ back to top](#top)

<a id="en-protocol"></a>
## Protocol

Plain-text messages over UDP: one message per datagram, UTF-8, 8 KB maximum.

| Message | Sender → receiver | Reply |
| --- | --- | --- |
| `create <nickname>` | peer → super-node | `OK` / `ERROR name already taken: <nickname>` |
| `heartbeat <nickname>` | peer → super-node (every 5 s) | none (an unknown peer is registered again) |
| `register <hash>` | peer → super-node | `REGISTERED <hash> at node <host:port>` / `ERROR invalid hash` / `ERROR no node owns <hash>` |
| `register_ring <hash> <peer_host> <peer_port> <origin_host> <origin_port>` | super-node → next | the owner replies to the peer |
| `list` | peer → super-node | `RESOURCES <n>` + one packet per `<hash>@<host>:<port>`, or `NO RESOURCE FOUND` |
| `list_ring <peer_host> <peer_port> <origin_host> <origin_port> [entries…]` | super-node → next | the origin replies to the peer |
| `resource <hash>` | peer → peer | `I HAVE THIS CONTENT!` + content / `I DO NOT HAVE THIS CONTENT...` |

[↑ back to top](#top)

<a id="en-structure"></a>
## Project structure

```
src/main/java/p2p/
  network/  UdpEndpoint, Packet, Message, MessageType          UDP transport and text protocol
  dht/      HashRange, ResourceTable, ResourceEntry, Md5        key-space partition and storage
  server/   SuperNode, SuperNodeConfig, PeerRegistry, PeerInfo  ring node and peer liveness
  peer/     PeerNode, PeerConsole, ConsoleCommand, PeerListener,
            HeartbeatSender, SharedResource, PeerConfig         peer process and console
src/test/java/p2p/                                              unit tests + in-process ring test
scripts/    start-ring.sh, stop-ring.sh, start-peer.sh          local demo helpers
```

[↑ back to top](#top)

<a id="en-getting-started"></a>
## Getting started

**Prerequisites:** JDK 21+ and Maven. At runtime only the JDK is used (`DatagramSocket`, `MessageDigest`, `BigInteger`, `ScheduledExecutorService`).

**Build and test**

```bash
mvn verify
```

**Start a ring of 3 super-nodes** on ports 9000–9002 (logs go to `logs/`)

```bash
scripts/start-ring.sh 3
```

**Start two peers**, each in its own terminal

```bash
scripts/start-peer.sh alice 5000 9000
scripts/start-peer.sh bob   5001 9002
```

**Stop the ring**

```bash
scripts/stop-ring.sh
```

Without the scripts:

```bash
java -cp target/classes p2p.server.SuperNode <port> <next_port> <ring_position> <ring_size> [next_host] [host]
java -cp target/classes p2p.peer.PeerNode <server_host> <server_port> <nickname> <peer_port>
```

**Peer console commands:** `register [server_host server_port]`, `list [server_host server_port]`, `resource <hash> <peer_host> <peer_port>`, `info`, `help`, `quit` / `exit`.

**Sample session** — bob's terminal; lines starting with `>` are what was typed:

```
Nickname: bob
Port:     5001
Hash:     481e07deebca04abcc1af46647db0e60
<- 127.0.0.1:9002: OK
> register
-> 127.0.0.1:9002: register 481e07deebca04abcc1af46647db0e60
<- 127.0.0.1:9000: REGISTERED 481e07deebca04abcc1af46647db0e60 at node 127.0.0.1:9000
> list
-> 127.0.0.1:9002: list
<- 127.0.0.1:9002: RESOURCES 2
<- 127.0.0.1:9002: 481e07deebca04abcc1af46647db0e60@127.0.0.1:5001
<- 127.0.0.1:9002: 5c3c6b720e53c55c7a93cc0cea0072c8@127.0.0.1:5000
> resource 5c3c6b720e53c55c7a93cc0cea0072c8 127.0.0.1 5000
-> 127.0.0.1:5000: resource 5c3c6b720e53c55c7a93cc0cea0072c8
<- 127.0.0.1:5000: I HAVE THIS CONTENT!
<- 127.0.0.1:5000: alice_4bbb6c4a-c2c3-4f0d-95b4-4fd607aceca8
```

Meanwhile, in the super-node logs:

```
[node 9000] Stored 481e07deebca04abcc1af46647db0e60@127.0.0.1:5001 (forwarded by the ring)
[node 9001] Stored 5c3c6b720e53c55c7a93cc0cea0072c8@127.0.0.1:5000 (forwarded by the ring)
[node 9000] Peer alice is INACTIVE
```

[↑ back to top](#top)

<a id="en-tests"></a>
## Tests

```bash
mvn verify
```

43 JUnit 5 tests cover the key-space partition (no gaps for ring sizes 1–7), message parsing, the peer registry (with a controllable clock for expiry), console parsing, and an **in-process 3-node ring over real UDP** that checks routing, `list`, malformed packets, duplicate nicknames and the loop guard. GitHub Actions runs the same command on every push.

[↑ back to top](#top)

<a id="en-changes"></a>
## What changed since v1

The version delivered for the course is frozen as the tag [`v1.0.0-original`](https://github.com/joaolaureano/java-p2p/tree/v1.0.0-original). v2.0.0 keeps the same design and features; it fixes every bug found in a review of v1, reorganizes the code and adds tests.

| # | Bug in v1 | Fix in v2 |
| --- | --- | --- |
| 1 | The server read the nickname argument before checking the packet length, so any one-word packet threw. | Every packet is parsed and validated by `Message.parse`; malformed ones are logged and ignored. |
| 2 | Heartbeat expiry ran inside `catch (Exception)`, triggered by receive timeouts *and* malformed packets: under traffic peers never expired, junk packets expired them faster. | Expiry is time-based and runs on its own scheduled thread, once per second. |
| 3 | The heartbeat counter could throw `NullPointerException` and never removed values that went below zero. | Counters were replaced by last-seen timestamps in `PeerRegistry`. |
| 4 | The partition left the tail of the MD5 space without an owner when `N` does not divide 2^128 (e.g. 3 nodes). | The last node owns everything up to 2^128 − 1 (`HashRange.forNode`). |
| 5 | `register_ring` never noticed it had come back to its origin: infinite loop for hashes nobody owns. | Ring messages carry their origin; the origin stops them and answers `ERROR no node owns <hash>`. |
| 6 | The next super-node was hard-coded as `localhost`, `list` was forwarded to the peer's address, and the final answer went to the previous hop instead of the peer: it only worked on one machine. | The next node's host is configurable, and ring answers are sent to the requesting peer. |
| 7 | Resource entries stored only the peer's port, not its host. | `ResourceEntry` stores hash, host and port. |
| 8 | Packet length used `String.length()` instead of the byte length (plus the platform charset), truncating non-ASCII text; the 1024-byte buffer truncated long lists. | UTF-8 everywhere, byte-accurate lengths, 8 KB datagrams with a size check. |
| 9 | Socket bind failures were swallowed (null socket, NPE later) and `receive` returned `null` on timeout. | `UdpEndpoint` fails fast with a clear message; `receive` returns `Optional.empty()` on timeout. |
| 10 | The peer console only caught `IOException`: empty or invalid input, or EOF, killed it. Help advertised a `peer` command that did not exist. | `ConsoleCommand` validates every line; errors are printed and the console keeps running. Help lists only real commands. |
| 11 | The heartbeat opened a third socket on a different port than the one it printed, and on error closed it and kept looping. | `HeartbeatSender` uses the peer's own socket and never closes it. |
| 12 | Heartbeats trusted the nickname only (anyone could keep someone else alive), expired peers were never registered again, and a rejected nickname was ignored by the peer. | Heartbeats are accepted only from the address:port that owns the nickname; unknown peers are registered again; a rejected nickname stops the peer. |
| 13 | The shared content was built from the server's host name instead of the nickname. | Content is `<nickname>_<uuid>`. |
| 14 | A debug line was printed on every hash, and the bucket's `get()` printed instead of returning. | No debug output; `ResourceTable.get` returns an `Optional`. |
| 15 | Incomplete Makefile, committed `.class` files, and launch scripts that required `gnome-terminal` + `jq` (Linux only). | Maven build, `.gitignore`, portable bash scripts, CI. |

How the v1 code maps to v2:

| v1 | v2 |
| --- | --- |
| `app/socket/Socket` | `network/UdpEndpoint`, `network/Packet` |
| `app/bucket/*` | `dht/HashRange`, `dht/ResourceTable`, `dht/ResourceEntry` |
| `app/command/*` | handler methods in `server/SuperNode` + `server/PeerRegistry` |
| `app/server/HostData` | `server/PeerInfo` |
| `app/peer/PeerClient`, `PeerThread`, `PeerHeartbeat` | `peer/PeerConsole`, `PeerListener`, `HeartbeatSender` |
| `app/resource_manager/*` | `peer/SharedResource`, `dht/Md5` |

[↑ back to top](#top)

<a id="en-limitations"></a>
## Known limitations

These are deliberate scope limits of the assignment, not bugs:

- UDP without retransmission: lost packets are not retried.
- Ring membership is static (configured at start); there is no join/leave protocol.
- A `list` answer must fit in one 8 KB datagram.
- No authentication.
- Resources of expired peers stay in the ring.

[↑ back to top](#top)

<a id="en-academic"></a>
## Academic context

This started as a college assignment in 2022. The delivered version is kept unchanged under the tag `v1.0.0-original`, so the two versions can be compared side by side.

[↑ back to top](#top)

<a id="en-license"></a>
## License

[MIT](LICENSE) © João Pedro Laureano

[↑ back to top](#top)
