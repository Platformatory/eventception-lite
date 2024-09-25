#!/bin/bash

FILE="order_id.txt"

if [ ! -f "$FILE" ]; then
  echo 0 > "$FILE"
fi

current_order_id=$(cat "$FILE")

new_order_id=$((current_order_id + 1))

while true; do
    quantity=$(( (RANDOM % 20) + 1 ))
    price=$(( (RANDOM % 5000) + 1 ))
    curl -X POST http://localhost:8000/orders -H "Content-Type:application/json" -d '{"id": "'$new_order_id'", "quantity": "'$quantity'", "product": "shirt", "price": "'$price'"}'
    echo "$new_order_id" > "$FILE"
    ((new_order_id++))
done
