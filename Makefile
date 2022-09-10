all:				p2pServer.class p2pPeer.class ContentManagement.class

p2pServer.class:		server/p2pServer.java
				@javac server/p2pServer.java

p2pPeer.class:			p2pPeerThread.class p2pPeerHeartbeat.class p2pPeerClient.class peer/p2pPeer.java
				@javac peer/p2pPeer.java

p2pPeerThread.class:		peer/p2pPeerThread.java
				@javac peer/p2pPeerThread.java
				
p2pPeerHeartbeat.class:		peer/p2pPeerHeartbeat.java
				@javac peer/p2pPeerHeartbeat.java

p2pPeerClient.class:		peer/p2pPeerClient.java
				@javac peer/p2pPeerClient.java
				
ContentManagement.class:	content_mngnt/ContentManagement.java
				@javac content_mngnt/ContentManagement.java

clean:
				@rm -rf *.class *~
