import os
import json
import subprocess
import argparse


result_on_gpt35 ={
    "closure":[15, 33, 55, 118, 161],
    "math":[10,25,58,75]
}
result_on_starcoder ={
    "closure":[5, 15, 19, 33, 36, 55, 161],
    "math":[10, 27, 48, 94],
}

result_on_codellama={
    "closure": [10, 15, 20, 33, 101, 113, 161],
    "lang":[14, 57]
}

result_on_llama={
    "closure": [5, 15],
    "lang":[55],
    "math":[58,85],
}

result_on_gpt35_20 = {
    "compress": [23, 27, 31],
    "codec": [4],
    "csv": [9],
    "jacksoncore":[20],
    # "jacksondatabind":[51],
    "jacksonxml":[5],
    # "jxpath":[21],
}

result_on_starcoder_20 = {
    "cli":[32],
    "codec":[4],
    "compress": [1, 27, 31, 46],
    # "jacksoncore":[3, 25],
    "jacksoncore":[25],
    # "jacksondatabind":[51],
    "jacksonxml":[5],
    "jsoup": [39]
}

result_on_codellama_20 = {
    "cli":[32],
    "codec":[4],
    "compress": [1],
    "jacksoncore":[5, 11],
    # "jacksondatabind":[46, 51, 102],
    "jacksonxml":[5],
    "jsoup": [24, 33]
}

result_on_llama_20 = {
    "cli":[32],
    "codec":[4],
    "compress": [1, 31],
    "jacksoncore":[7, 11],
    # "jacksondatabind":[24]
    # "jacksonxml":[5]
}

def get_bug_ids(llmName, version):
    if llmName == "gpt35":
        if version == "10":
            result_dict = result_on_gpt35
        elif version == "20":
            result_dict = result_on_gpt35_20
    elif llmName == "starcoder":
        if version == "10":
            result_dict = result_on_starcoder
        elif version == "20":
            result_dict = result_on_starcoder_20
    elif llmName == "codellama":
        if version == "10":
            result_dict = result_on_codellama
        elif version == "20":
            result_dict = result_on_codellama_20
    elif llmName == "llama":
        if version == "10":
            result_dict = result_on_llama
        elif version == "20":
            result_dict = result_on_llama_20
    bugids = []
    for key in result_dict.keys():
        for idnum in result_dict[key]:
            bugids.append("{}_{}".format(key, idnum))
    return bugids

def run_apr_tools(llmName, version):
    
    bugids = get_bug_ids(llmName, version)
    
    RUN_APR_TOOL_CMD = "java -jar GenPat.jar repair -d4j {bugid} -d4jhome /data/PLM4APR/tmp/defects4j_buggy/ -modelname {modelName}"

    RUN_SUCCESS_BUGS = []
    RUN_FAILED_BUGS = []

    ret_dict = {}
    for bugid in bugids:
        print("Current fix is {}".format(bugid))
        result = subprocess.run(RUN_APR_TOOL_CMD.format(bugid=bugid, modelName=llmName), shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
        if result.returncode != -1:
            proj, idnum = bugid.split("_")
            log_path = "./result/log/{proj}/{num}.txt".format(proj=proj, num=idnum)
            lines = open(log_path, "r").readlines()
            result_on_one_bug = []
            for line in lines:
                if line.startswith("All action amount is:"):
                    amount = int(line.strip().split("All action amount is:")[1])
                    result_on_one_bug.append(amount)
            ret_dict[bugid] = result_on_one_bug
        else:
            RUN_FAILED_BUGS.append(bugid)
            print("Failed to run APR tool on bugid: " + bugid)
            print(result.stdout)

    print("RUN_SUCCESS_BUGS: " + str(RUN_SUCCESS_BUGS))
    print("RUN_FAILED_BUGS: " + str(RUN_FAILED_BUGS))
    return ret_dict

def get_target_index(llmName, version):
    if llmName == "gpt35":
        llmName = "gpt35_new"
    if version == "10":
        file_path = "/data/PLM4APR/codex_out/valuable_patches_{llm}.txt".format(llm=llmName)
    else:
        file_path = "/data/PLM4APR/codex_out/valuable_patches_{llm}_{version}.txt".format(llm=llmName, version = version)
    lines = open(file_path).readlines()

    ret_dict = {}
    for line in lines:
        line = line.strip()
        if len(line) == 0:
            continue
        proj, idnum, index = line.split("_")
        ret_dict[proj.lower()+"_"+idnum] = int(index)
    return ret_dict


if __name__ == "__main__":

    # LLMs = ["gpt35", "starcoder", "codellama", "llama"]
    LLMs = ["starcoder"]
    versions = ["20"]
    with open("./amount_20.txt", "w") as f:
        for llm in LLMs:
            for version in versions:

                target_indexes = get_target_index(llm, version)
                result = run_apr_tools(llm, version)
                to_str = {}
                for key in result:
                    index = target_indexes[key]
                    to_str[key] = result[key][index]

                json_str = json.dumps(to_str, indent=4)
                f.write(llm+"_"+version)
                f.write(json_str)
                f.write("\n")