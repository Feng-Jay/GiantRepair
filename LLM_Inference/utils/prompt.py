# Used for completion prompts

VARY_BASE_PROMPT = """# Provide a fix for the buggy function

# Buggy Function
def fibonacci(n):
    if n == 0:
        return 0
    elif n == 1 or n == 2:
        return 1
    else:
        return fibonacci(n-1) - fibonacci(n-2)

# Fixed Function
def fibonacci(n):
    if n == 0:
        return 0
    elif n == 1 or n == 2:
        return 1
    else:
        return fibonacci(n-1) + fibonacci(n-2)

# Provide a fix for the buggy function

# Buggy Function
{example_bug}

# Fixed Function
{example_fix}

# Provide a fix for the buggy function

# Buggy Function
{bug}

# Fixed Function
"""


JAVA_LONG_VARY_PROMPT = """// Provide a fix for the buggy function

// Buggy Function
int binarySearch(int arr[], int l, int r, int x)
{{
    if (r >= l) {{
        int mid = l + (r + l) / 2;
        if (arr[mid] == x)
            return mid;
        if (arr[mid] > x)
            return binarySearch(arr, l, mid - 1, x);
        return binarySearch(arr, mid + 1, r, x);
    }}
    return -1;
}}

// Fixed Function
int binarySearch(int arr[], int l, int r, int x)
{{
    if (r >= l) {{
        int mid = l + (r - l) / 2;
        if (arr[mid] == x)
            return mid;
        if (arr[mid] > x)
            return binarySearch(arr, l, mid - 1, x);
        return binarySearch(arr, mid + 1, r, x);
    }}
    return -1;
}}

// Provide a fix for the buggy function

// Buggy Function
{example_bug}

// Fixed Function
{example_fix}

// Provide a fix for the buggy function

// Buggy Function
{bug}

// Fixed Function
"""


C_VARY_PROMPT = """/* Provide a fix for the buggy function */

/* Buggy Function */
int binarySearch(int arr[], int l, int r, int x)
{{
    if (r >= l) {{
        int mid = l + (r + l) / 2;
        if (arr[mid] == x)
            return mid;
        if (arr[mid] > x)
            return binarySearch(arr, l, mid - 1, x);
        return binarySearch(arr, mid + 1, r, x);
    }}
    return -1;
}}

/* Fixed Function */
int binarySearch(int arr[], int l, int r, int x)
{{
    if (r >= l) {{
        int mid = l + (r - l) / 2;
        if (arr[mid] == x)
            return mid;
        if (arr[mid] > x)
            return binarySearch(arr, l, mid - 1, x);
        return binarySearch(arr, mid + 1, r, x);
    }}
    return -1;
}}

/* Provide a fix for the buggy function */

/* Buggy Function */
{example_bug}

/* Fixed Function */
{example_fix}

/* Provide a fix for the buggy function */

/* Buggy Function */
{bug}

/* Fixed Function */
"""


