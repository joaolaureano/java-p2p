all:				Server.class Socket.class HostData.class ICommand.class CreateCommand.class ListCommand.class HeartBeatCommand.class Peer.class PeerClient.class PeerHeartbeat.class PeerThread.class

Server.class:		app/server/Server.java
				@javac app/server/Server.java

Socket.class:		app/socket/Socket.java
				@javac app/socket/Socket.java

HostData.class:		app/server/HostData.java
				@javac app/server/HostData.java
				
ICommand.class:		app/command/ICommand.java
				@javac app/command/ICommand.java

CreateCommand.class:		app/command/CreateCommand.java
					@javac app/command/CreateCommand.java

HeartBeatCommand.class:		app/command/HeartBeatCommand.java
					@javac app/command/HeartBeatCommand.java

ListCommand.class:		app/command/ListCommand.java
					@javac app/command/ListCommand.java

Peer.class:			app/peer/Peer.java
					@javac app/peer/Peer.java

PeerClient.class:	app/peer/PeerClient.java
					@javac app/peer/PeerClient.java

PeerHeartbeat.class:	app/peer/PeerHeartbeat.java
						@javac app/peer/PeerHeartbeat.java

PeerThread.class:		app/peer/PeerThread.java
						@javac app/peer/PeerThread.java

clean:
				@rm -rf *.class *~
