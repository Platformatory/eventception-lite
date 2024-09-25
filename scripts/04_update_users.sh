#!/bin/bash

FILE="user_id.txt"

if [ ! -f "$FILE" ]; then
  echo 0 > "$FILE"
fi

latest_user_id=$(cat "$FILE")

while true; do 
    age=$(( (RANDOM % 70) + 1 ))
    user_id=$(( (RANDOM % latest_user_id) + 1 ))
    curl -X PUT http://localhost:8000/users -H "Content-Type:application/json" -d '{"id": "'$user_id'", "age": "'$age'", "name": "jane doe"}'; 
done
