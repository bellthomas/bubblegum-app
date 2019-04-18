#!/bin/bash

# Start the proxy client if required
if [ -z ${FRP_SERVER_ADDR+x} ]; then 
	echo "Not starting the FRP proxy client."; 
else 
	echo "Starting the FRP proxy client";
	chmod +x /frpc
	/frpc -c /frpc.ini &  
fi

/app/bin/start
