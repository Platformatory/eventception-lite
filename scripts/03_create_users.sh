#!/bin/bash

FILE="user_id.txt"

if [ ! -f "$FILE" ]; then
  echo 0 > "$FILE"
fi

current_user_id=$(cat "$FILE")

new_user_id=$((current_user_id + 1))

while true; do
    age=$(( (RANDOM % 70) + 1 ))
    curl -X POST http://localhost/users -H "Content-Type:application/json" -d '{"id": "'$new_user_id'", "age": "'$age'", "name": "john doe"}'
    echo "$new_user_id" > "$FILE"
    ((new_user_id++))
done
