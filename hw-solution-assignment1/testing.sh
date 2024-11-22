# Test 1
result=$(java -cp 'antlr-4.13.2-complete.jar:.' main ./test1_xor.hw)
expected="0010100 A
0011000 B"
if [[ "$result" == "$expected" ]]; then
	echo "Test 1 passed successfully"
else
	echo "Error: expected: $expected, got: $result"
fi

# Test 2
result=$(java -cp 'antlr-4.13.2-complete.jar:.' main ./test2_multiple_defs.hw)
expected="0010100 A
1100110 B
0110011 C"
if [[ "$result" == "$expected" ]]; then
	echo "Test 2 passed successfully"
else
	echo "Error: expected $expected, got $result"
fi
