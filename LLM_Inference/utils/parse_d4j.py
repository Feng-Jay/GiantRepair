import json

d4j_bug_lists = '''
| Chart           | jfreechart                 |       26       | 1-26                | None                    |
| Cli             | commons-cli                |       39       | 1-5,7-40            | 6                       |
| Closure         | closure-compiler           |      174       | 1-62,64-92,94-176   | 63,93                   |
| Codec           | commons-codec              |       18       | 1-18                | None                    |
| Collections     | commons-collections        |        4       | 25-28               | 1-24                    |
| Compress        | commons-compress           |       47       | 1-47                | None                    |
| Csv             | commons-csv                |       16       | 1-16                | None                    |
| Gson            | gson                       |       18       | 1-18                | None                    |
| JacksonCore     | jackson-core               |       26       | 1-26                | None                    |
| JacksonDatabind | jackson-databind           |      112       | 1-112               | None                    |
| JacksonXml      | jackson-dataformat-xml     |        6       | 1-6                 | None                    |
| Jsoup           | jsoup                      |       93       | 1-93                | None                    |
| JxPath          | commons-jxpath             |       22       | 1-22                | None                    |
| Lang            | commons-lang               |       64       | 1,3-65              | 2                       |
| Math            | commons-math               |      106       | 1-106               | None                    |
| Mockito         | mockito                    |       38       | 1-38                | None                    |
| Time            | joda-time                  |       26       | 1-20,22-27          | 21                      |'''

def _get_relevant_bugs(bugs, current_bug, only_same):
    potential_pairs = []
    # items = current_bug.split("-")
    # current_bug = items[0] + "-" + items[1] + ".java"
    project = current_bug.split("-")[0]
    for file_name, bug in bugs.items():
        if file_name == current_bug:
            continue
        if file_name.startswith(project + "-") and only_same:
            potential_pairs.append((len(bug['buggy']) + len(bug['fix']), file_name))
        elif not only_same:
            potential_pairs.append((len(bug['buggy']) + len(bug['fix']), file_name))
    # sort from smallest to largest
    potential_pairs.sort(key=lambda x: x[0])
    return potential_pairs


# picking an example fix pairs from a project
def pick_smallest_example_fix(bugs, current_bug, only_same=False):
    potential_pairs = _get_relevant_bugs(bugs, current_bug, only_same)
    return bugs[potential_pairs[0][1]]['buggy'], bugs[potential_pairs[0][1]]['fix']

def _get_relevant_bugs_topN(bugs, current_bug, only_same):
    potential_pairs = []
    items = current_bug.split("-") # Lang-34-1.java
    current_bug = items[0] + "-" + items[1] + ".java" 
    project = current_bug.split("-")[0]
    for file_name, bug in bugs.items():
        if file_name == current_bug:
            continue
        if file_name.startswith(project + "-") and only_same:
            potential_pairs.append((len(bug['buggy']) + len(bug['fix']), file_name))
        elif not only_same:
            potential_pairs.append((len(bug['buggy']) + len(bug['fix']), file_name))
    # sort from smallest to largest
    potential_pairs.sort(key=lambda x: x[0])
    return potential_pairs

def pick_smallest_example_fix_topN(bugs, current_bug, only_same=False):
    potential_pairs = _get_relevant_bugs_topN(bugs, current_bug, only_same)
    return bugs[potential_pairs[0][1]]['buggy'], bugs[potential_pairs[0][1]]['fix'] 

def clean_parse_d4j_single_hunk(folder):
    with open(folder + "/single_function_single_hunk_repair.json", "r") as f:
        result = json.load(f)
    cleaned_result = {}
    for k, v in result.items():
        lines = v['buggy'].splitlines()
        leading_white_space = len(lines[0]) - len(lines[0].lstrip())
        cleaned_result[k + ".java"] = {"buggy": "\n".join([line[leading_white_space:] for line in lines])}
        lines = v["prefix"].splitlines()
        cleaned_result[k + ".java"]["prefix"] = "\n".join([line[leading_white_space:] for line in lines])
        lines = v["suffix"].splitlines()
        cleaned_result[k + ".java"]["suffix"] = "\n".join([line[leading_white_space:] for line in lines])
        lines = v['fix'].splitlines()
        leading_white_space = len(lines[0]) - len(lines[0].lstrip())
        cleaned_result[k + ".java"]["fix"] = "\n".join([line[leading_white_space:] for line in lines])
    return cleaned_result

def clean_parse_d4j_expand(folder):
    with open(folder + "d4j-info/growing_bugs_single_function_expand.json", "r") as f:
        result = json.load(f)
    cleaned_result = {}
    # defects4j_v1_0 = ["Chart", "Closure", "Lang", "Math", "Time"]
    # defects4j_v2_0 = ["Cli", "Codec", "Collections", "Compress", "Csv", "Gson", "JacksonCore", "JacksonDatabind", "JacksonXml", "Jsoup", "JxPath", "Mockito"]
    for k, v in result.items():
        # if(k.split("-")[0] not in defects4j_v2_0):
        #     continue
        lines = v['buggy'].splitlines()
        leading_white_space = len(lines[0]) - len(lines[0].lstrip())
        cleaned_result[k + ".java"] = {"buggy": "\n".join([line[leading_white_space:] for line in lines])}
        lines = v['fix'].splitlines()
        leading_white_space = len(lines[0]) - len(lines[0].lstrip())
        cleaned_result[k + ".java"]["fix"] = "\n".join([line[leading_white_space:] for line in lines])
    # print(cleaned_result['JacksonDatabind-17.java']['buggy'])
    return cleaned_result

def clean_parse_d4j(folder):
    with open(folder + "d4j-info/growing_bugs_single_function.json", "r") as f:
        result = json.load(f)
    cleaned_result = {}
    # defects4j_v1_0 = ["Chart", "Closure", "Lang", "Math", "Time"]
    # defects4j_v2_0 = ["Cli", "Codec", "Collections", "Compress", "Csv", "Gson", "JacksonCore", "JacksonDatabind", "JacksonXml", "Jsoup", "JxPath", "Mockito"]
    for k, v in result.items():
        # if(k.split("-")[0] not in defects4j_v2_0):
        #     continue
        lines = v['buggy'].splitlines()
        leading_white_space = len(lines[0]) - len(lines[0].lstrip())
        cleaned_result[k + ".java"] = {"buggy": "\n".join([line[leading_white_space:] for line in lines])}
        lines = v['fix'].splitlines()
        leading_white_space = len(lines[0]) - len(lines[0].lstrip())
        cleaned_result[k + ".java"]["fix"] = "\n".join([line[leading_white_space:] for line in lines])
    # print(cleaned_result['JacksonDatabind-17.java']['buggy'])
    return cleaned_result

def clean_parse_d4j_topN(folder):
    with open(folder + "d4j-info/top_n_function.json", "r") as f:
        result = json.load(f)
    cleaned_result = {}
    for k, v in result.items():
        for index, method in enumerate(v):
            lines = method['buggy'].splitlines()
            leading_white_space = len(lines[0]) - len(lines[0].lstrip())
            key = k + "-" + str(index) +".java"
            cleaned_result[key] = {"buggy": "\n".join([line[leading_white_space:] for line in lines])}
    return cleaned_result



def clean_parse_d4j_single_line(folder):
    with open(folder + "Defects4j/single_function_single_line_repair.json", "r") as f:
        result = json.load(f)
    cleaned_result = {}
    for k, v in result.items():
        lines = v['buggy'].splitlines()
        leading_white_space = len(lines[0]) - len(lines[0].lstrip())
        cleaned_result[k + ".java"] = {"buggy": "\n".join([line[leading_white_space:] for line in lines])}
        lines = v["prefix"].splitlines()
        cleaned_result[k + ".java"]["prefix"] = "\n".join([line[leading_white_space:] for line in lines])
        lines = v["suffix"].splitlines()
        cleaned_result[k + ".java"]["suffix"] = "\n".join([line[leading_white_space:] for line in lines])
        lines = v['fix'].splitlines()
        leading_white_space = len(lines[0]) - len(lines[0].lstrip())
        cleaned_result[k + ".java"]["fix"] = "\n".join([line[leading_white_space:] for line in lines])

        buggy_line = cleaned_result[k + ".java"]["buggy"] \
            .removeprefix(cleaned_result[k + ".java"]["prefix"]).removesuffix(
            cleaned_result[k + ".java"]["suffix"]).replace("\n", "")
        cleaned_result[k + ".java"]["buggy_line"] = buggy_line
    return cleaned_result