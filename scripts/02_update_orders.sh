#!/bin/bash

FILE="order_id.txt"

if [ ! -f "$FILE" ]; then
  echo 0 > "$FILE"
fi

latest_order_id=$(cat "$FILE")

while true; do 
    quantity=$(( (RANDOM % 20) + 1 ))
    price=$(( (RANDOM % 5000) + 1 ))
    order_id=$(( (RANDOM % latest_order_id) + 1 ))
    curl -X PUT http://localhost:8000/orders -H "Content-Type:application/json" -d '{"id": "'$order_id'", "quantity": "'$quantity'", "product": "shirt", "price": "'$price'"}'; 
done
