all:				Server.class Socket.class HostData.class ICommand.class CreateCommand.class ListCommand.class HeartBeatCommand.class Peer.class PeerClient.class PeerHeartbeat.class PeerThread.class

Server.class:		server/Server.java
				@javac server/Server.java

Socket.class:		server/Socket.java
				@javac server/Socket.java

HostData.class:		server/HostData.java
				@javac server/HostData.java
				
ICommand.class:		server/command/ICommand.java
				@javac server/command/ICommand.java

CreateCommand.class:		server/command/CreateCommand.java
					@javac server/command/CreateCommand.java

HeartBeatCommand.class:		server/command/HeartBeatCommand.java
					@javac server/command/HeartBeatCommand.java

ListCommand.class:		server/command/ListCommand.java
					@javac server/command/ListCommand.java

Peer.class:			server/peer/Peer.java
					@javac server/peer/Peer.java

PeerClient.class:	server/peer/PeerClient.java
					@javac server/peer/PeerClient.java

PeerHeartbeat.class:	server/peer/PeerHeartbeat.java
						@javac server/peer/PeerHeartbeat.java

PeerThread.class:		server/peer/PeerThread.java
						@javac server/peer/PeerThread.java

clean:
				@rm -rf *.class *~
