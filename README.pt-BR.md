<a id="top"></a>
<h1 align="center">java-p2p</h1>

<p align="center">Uma rede peer-to-peer sobre UDP em que um anel de super-nodes particiona o espaço de chaves MD5 — uma pequena DHT escrita do zero em Java 21, sem nenhuma dependência em tempo de execução.</p>

<p align="center">
  <a href="https://github.com/joaolaureano/java-p2p/actions/workflows/ci.yml"><img alt="CI" src="https://github.com/joaolaureano/java-p2p/actions/workflows/ci.yml/badge.svg"></a>
  <img alt="Java 21" src="https://img.shields.io/badge/Java-21-orange">
  <img alt="Maven" src="https://img.shields.io/badge/build-Maven-blue">
  <img alt="JUnit 5" src="https://img.shields.io/badge/tests-JUnit%205-green">
  <img alt="MIT License" src="https://img.shields.io/badge/license-MIT-yellow">
</p>

<p align="center"><a href="README.md">🇺🇸 English</a> · <b>🇧🇷 Português</b></p>

---

## Sumário

- [Visão geral](#pt-visao-geral)
- [Arquitetura](#pt-arquitetura)
- [Como a DHT funciona](#pt-dht)
- [Protocolo](#pt-protocolo)
- [Estrutura do projeto](#pt-estrutura)
- [Como rodar](#pt-como-rodar)
- [Testes](#pt-testes)
- [O que mudou desde a v1](#pt-mudancas)
- [Limitações conhecidas](#pt-limitacoes)
- [Contexto acadêmico](#pt-contexto)
- [Licença](#pt-licenca)

<a id="pt-visao-geral"></a>
## Visão geral

java-p2p é uma rede peer-to-peer sobre UDP. Um anel de super-nodes divide o espaço de chaves MD5 (2^128) em fatias iguais — uma tabela hash distribuída (DHT) simples.

Cada peer se registra em um super-node (`create <nickname>`), se mantém vivo com heartbeats a cada 5 s (expira após 15 s de silêncio), publica o hash MD5 do conteúdo que compartilha (`register`), pode listar todos os recursos do anel (`list`) e pede o conteúdo diretamente a outro peer (`resource <hash> <host> <port>`). Requisições que o super-node atual não possui percorrem o anel (`register_ring`, `list_ring`) levando a origem, e param quando voltam a ela.

[↑ voltar ao topo](#top)

<a id="pt-arquitetura"></a>
## Arquitetura

```mermaid
flowchart LR
    SN1["super-node 1 :9000"] --> SN2["super-node 2 :9001"]
    SN2 --> SN3["super-node 3 :9002"]
    SN3 --> SN1
    alice(["peer alice"]) --> SN1
    bob(["peer bob"]) --> SN3
    bob -. "resource hash" .-> alice
```

Três super-nodes formam o anel. Cada um é dono de uma fatia do espaço de hashes, e cada peer se conecta a um único super-node para `create`, `heartbeat`, `register` e `list`. O conteúdo em si nunca passa pelo anel: um peer pede diretamente ao peer que o possui, com `resource <hash>`.

[↑ voltar ao topo](#top)

<a id="pt-dht"></a>
## Como a DHT funciona

Cada recurso é identificado pelo hash MD5 do seu conteúdo. O anel divide o espaço de 2^128 chaves em `N` fatias iguais:

```
slice = 2^128 / N
node i owns [slice * (i - 1), slice * i - 1]
the last node owns up to 2^128 - 1
```

Como o último node fica com o resto, nenhum hash fica sem dono, mesmo quando `N` não divide 2^128 exatamente.

Para um anel de 3 nodes:

| Node | Porta | Faixa de hashes |
| --- | --- | --- |
| super-node 1 | 9000 | `0000…0` – `5555…4` |
| super-node 2 | 9001 | `5555…5` – `aaaa…9` |
| super-node 3 | 9002 | `aaaa…a` – `ffff…f` |

Cada super-node registra no log a faixa que possui ao iniciar:

```
[node 9000] Owns HashRange[00000000000000000000000000000000 .. 55555555555555555555555555555554]
```

[↑ voltar ao topo](#top)

<a id="pt-protocolo"></a>
## Protocolo

Mensagens de texto sobre UDP: uma mensagem por datagrama, UTF-8, máximo de 8 KB.

| Mensagem | Origem → destino | Resposta |
| --- | --- | --- |
| `create <nickname>` | peer → super-node | `OK` / `ERROR name already taken: <nickname>` |
| `heartbeat <nickname>` | peer → super-node (a cada 5 s) | nenhuma (peer desconhecido é registrado de novo) |
| `register <hash>` | peer → super-node | `REGISTERED <hash> at node <host:port>` / `ERROR invalid hash` / `ERROR no node owns <hash>` |
| `register_ring <hash> <peer_host> <peer_port> <origin_host> <origin_port>` | super-node → próximo | o dono responde ao peer |
| `list` | peer → super-node | `RESOURCES <n>` + um pacote por `<hash>@<host>:<port>`, ou `NO RESOURCE FOUND` |
| `list_ring <peer_host> <peer_port> <origin_host> <origin_port> [entries…]` | super-node → próximo | a origem responde ao peer |
| `resource <hash>` | peer → peer | `I HAVE THIS CONTENT!` + conteúdo / `I DO NOT HAVE THIS CONTENT...` |

[↑ voltar ao topo](#top)

<a id="pt-estrutura"></a>
## Estrutura do projeto

```
src/main/java/p2p/
  network/  UdpEndpoint, Packet, Message, MessageType          transporte UDP e protocolo de texto
  dht/      HashRange, ResourceTable, ResourceEntry, Md5        partição do espaço de chaves e armazenamento
  server/   SuperNode, SuperNodeConfig, PeerRegistry, PeerInfo  node do anel e controle de peers ativos
  peer/     PeerNode, PeerConsole, ConsoleCommand, PeerListener,
            HeartbeatSender, SharedResource, PeerConfig         processo do peer e console
src/test/java/p2p/                                              testes unitários + teste do anel em processo
scripts/    start-ring.sh, stop-ring.sh, start-peer.sh          scripts para a demo local
```

[↑ voltar ao topo](#top)

<a id="pt-como-rodar"></a>
## Como rodar

**Pré-requisitos:** JDK 21+ e Maven. Em execução, só o JDK é usado (`DatagramSocket`, `MessageDigest`, `BigInteger`, `ScheduledExecutorService`).

**Build e testes**

```bash
mvn verify
```

**Subir um anel de 3 super-nodes** nas portas 9000–9002 (logs em `logs/`)

```bash
scripts/start-ring.sh 3
```

**Subir dois peers**, cada um em um terminal

```bash
scripts/start-peer.sh alice 5000 9000
scripts/start-peer.sh bob   5001 9002
```

**Derrubar o anel**

```bash
scripts/stop-ring.sh
```

Sem os scripts:

```bash
java -cp target/classes p2p.server.SuperNode <port> <next_port> <ring_position> <ring_size> [next_host] [host]
java -cp target/classes p2p.peer.PeerNode <server_host> <server_port> <nickname> <peer_port>
```

**Comandos do console do peer:** `register [server_host server_port]`, `list [server_host server_port]`, `resource <hash> <peer_host> <peer_port>`, `info`, `help`, `quit` / `exit`.

**Sessão de exemplo** — terminal do bob; linhas começando com `>` foram digitadas:

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

Enquanto isso, nos logs dos super-nodes:

```
[node 9000] Stored 481e07deebca04abcc1af46647db0e60@127.0.0.1:5001 (forwarded by the ring)
[node 9001] Stored 5c3c6b720e53c55c7a93cc0cea0072c8@127.0.0.1:5000 (forwarded by the ring)
[node 9000] Peer alice is INACTIVE
```

[↑ voltar ao topo](#top)

<a id="pt-testes"></a>
## Testes

```bash
mvn verify
```

43 testes JUnit 5 cobrem a partição do espaço de chaves (sem lacunas para anéis de 1 a 7 nodes), o parsing das mensagens, o registro de peers (com relógio controlável para testar a expiração), o parsing do console e um **anel de 3 nodes em processo, sobre UDP real**, que verifica roteamento, `list`, pacotes malformados, nicknames duplicados e a proteção contra loop. O GitHub Actions roda o mesmo comando a cada push.

[↑ voltar ao topo](#top)

<a id="pt-mudancas"></a>
## O que mudou desde a v1

A versão entregue na disciplina está congelada na tag [`v1.0.0-original`](https://github.com/joaolaureano/java-p2p/tree/v1.0.0-original). A v2.0.0 mantém o mesmo design e as mesmas funcionalidades; corrige todos os bugs encontrados numa revisão da v1, reorganiza o código e adiciona testes.

| # | Bug na v1 | Correção na v2 |
| --- | --- | --- |
| 1 | O servidor lia o nickname antes de checar o tamanho do pacote; qualquer pacote de uma palavra lançava exceção. | Todo pacote é interpretado e validado por `Message.parse`; os malformados são registrados no log e ignorados. |
| 2 | A expiração por heartbeat rodava dentro de `catch (Exception)`, disparada por timeouts de recebimento *e* por pacotes malformados: com tráfego os peers nunca expiravam, e pacotes inválidos os expiravam mais rápido. | A expiração é baseada em tempo e roda numa thread agendada própria, a cada segundo. |
| 3 | O contador de heartbeat podia lançar `NullPointerException` e nunca removia valores abaixo de zero. | Os contadores deram lugar a timestamps de "visto por último" no `PeerRegistry`. |
| 4 | A partição deixava a cauda do espaço MD5 sem dono quando `N` não divide 2^128 (ex.: 3 nodes). | O último node é dono de tudo até 2^128 − 1 (`HashRange.forNode`). |
| 5 | `register_ring` não percebia que tinha voltado à origem: loop infinito para hashes sem dono. | As mensagens do anel levam a origem; a origem as interrompe e responde `ERROR no node owns <hash>`. |
| 6 | O próximo super-node era fixo em `localhost`, o `list` era encaminhado para o endereço do peer e a resposta final ia para o salto anterior em vez do peer: só funcionava numa máquina. | O host do próximo node é configurável, e as respostas do anel vão para o peer que pediu. |
| 7 | As entradas de recurso guardavam só a porta do peer, não o host. | `ResourceEntry` guarda hash, host e porta. |
| 8 | O tamanho do pacote usava `String.length()` em vez do tamanho em bytes (e o charset da plataforma), truncando texto não-ASCII; o buffer de 1024 bytes truncava listas longas. | UTF-8 em todo lugar, tamanhos exatos em bytes, datagramas de 8 KB com verificação de tamanho. |
| 9 | Falhas de bind do socket eram engolidas (socket nulo, NPE depois) e o `receive` retornava `null` no timeout. | `UdpEndpoint` falha logo com mensagem clara; `receive` retorna `Optional.empty()` no timeout. |
| 10 | O console do peer só tratava `IOException`: entrada vazia ou inválida, ou EOF, o derrubavam. O help anunciava um comando `peer` que não existia. | `ConsoleCommand` valida cada linha; erros são mostrados e o console continua. O help lista só comandos reais. |
| 11 | O heartbeat abria um terceiro socket numa porta diferente da que imprimia e, em caso de erro, o fechava e continuava no loop. | `HeartbeatSender` usa o próprio socket do peer e nunca o fecha. |
| 12 | O heartbeat confiava só no nickname (qualquer um mantinha outro vivo), peers expirados nunca voltavam a ser registrados e um nickname rejeitado era ignorado pelo peer. | Heartbeats só são aceitos do endereço:porta dono do nickname; peers desconhecidos são registrados de novo; um nickname rejeitado encerra o peer. |
| 13 | O conteúdo compartilhado era montado com o host do servidor em vez do nickname. | O conteúdo é `<nickname>_<uuid>`. |
| 14 | Uma linha de debug era impressa a cada hash, e o `get()` do bucket imprimia em vez de retornar. | Sem saída de debug; `ResourceTable.get` retorna um `Optional`. |
| 15 | Makefile incompleto, arquivos `.class` versionados e scripts que exigiam `gnome-terminal` + `jq` (só Linux). | Build com Maven, `.gitignore`, scripts bash portáveis, CI. |

Como o código da v1 foi reorganizado na v2:

| v1 | v2 |
| --- | --- |
| `app/socket/Socket` | `network/UdpEndpoint`, `network/Packet` |
| `app/bucket/*` | `dht/HashRange`, `dht/ResourceTable`, `dht/ResourceEntry` |
| `app/command/*` | métodos de tratamento em `server/SuperNode` + `server/PeerRegistry` |
| `app/server/HostData` | `server/PeerInfo` |
| `app/peer/PeerClient`, `PeerThread`, `PeerHeartbeat` | `peer/PeerConsole`, `PeerListener`, `HeartbeatSender` |
| `app/resource_manager/*` | `peer/SharedResource`, `dht/Md5` |

[↑ voltar ao topo](#top)

<a id="pt-limitacoes"></a>
## Limitações conhecidas

São limites de escopo do trabalho, não bugs:

- UDP sem retransmissão: pacotes perdidos não são reenviados.
- A composição do anel é estática (definida na inicialização); não há protocolo de entrada/saída de nodes.
- A resposta de um `list` precisa caber num datagrama de 8 KB.
- Sem autenticação.
- Recursos de peers expirados continuam no anel.

[↑ voltar ao topo](#top)

<a id="pt-contexto"></a>
## Contexto acadêmico

O projeto nasceu como trabalho de faculdade em 2022. A versão entregue continua intacta na tag `v1.0.0-original`, para que as duas versões possam ser comparadas lado a lado.

[↑ voltar ao topo](#top)

<a id="pt-licenca"></a>
## Licença

[MIT](LICENSE) © João Pedro Laureano

[↑ voltar ao topo](#top)
