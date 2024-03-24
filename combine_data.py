import json
import os
import subprocess
from difflib import unified_diff
import sys

LLMS = ["gpt", "starcoder", "codellama", "llama"]
GENERATION_LLMS = ["gpt35", "starcoderbase", "CodeLlama-7b-hf", "Llama-2-13b-hf_200"]

# RESULT_BASE_PATH = "/Users/ffengjay/Postgraduate/PLM4APR/APR-LLM/results/baselines/"
# GENERATION_BASE_PATH = "/Users/ffengjay/Postgraduate/PLM4APR/APR-LLM/results/"

FIXES_12_PATH = "/Users/ffengjay/Postgraduate/PLM4APR/APR-LLM/results/baselines/{llm}_top.txt"
FIXES_20_PATH = "/Users/ffengjay/Postgraduate/PLM4APR/APR-LLM/results/baselines/{llm}_20_top.txt"

TEST_12_PATH = "/Users/ffengjay/Postgraduate/PLM4APR/APR-LLM/results/{llm}_test/{proj}_{idnum}.txt"
TEST_20_PATH = "/Users/ffengjay/Postgraduate/PLM4APR/APR-LLM/results/{llm}_20_test/{proj}_{idnum}.txt"

# TOP_12_FILE = "/Users/ffengjay/Postgraduate/PLM4APR/APR-LLM/results/baselines/{llm}_top.txt"
# TOP_20_FILE = "/Users/ffengjay/Postgraduate/PLM4APR/APR-LLM/results/baselines/{llm}_20_top.txt"

GENERATION_12_PATH = "/Users/ffengjay/Postgraduate/PLM4APR/APR-LLM/results/{llm}/{proj}-{idnum}.json"
GENERATION_20_PATH = "/Users/ffengjay/Postgraduate/PLM4APR/APR-LLM/results/{llm}_20/{proj}-{idnum}.json"

base_json = "/Users/ffengjay/Postgraduate/PLM4APR/d4j-info/single_function_repair.json"
base_dict = json.load(open(base_json, 'r'))

def get_unified_diff(source, mutant):
    output = ""
    for line in unified_diff(source.split('\n'), mutant.split('\n'), lineterm=''):
        output += line + "\n"
    return output

def compare(llm, proj, idnum, rank):
    buggy = base_dict[proj + "-" + idnum]["buggy"]

    test_12_path = TEST_12_PATH.format(llm=GENERATION_LLMS[LLMS.index(llm)], proj=proj, idnum=idnum)
    test_20_path = TEST_20_PATH.format(llm=GENERATION_LLMS[LLMS.index(llm)], proj=proj, idnum=idnum)
    patch_ids = []
    if os.path.exists(test_12_path):
        lines = open(test_12_path).readlines()
        for line in lines:
            line = line.strip()
            if line.startswith("No.") and line.endswith(" Patch"):
                id = line.split("No.")[1].split(" Patch")[0]
                id = int(id)
                patch_ids.append(id)
    elif os.path.exists(test_20_path):
        lines = open(test_20_path).readlines()
        for line in lines:
            line = line.strip()
            if line.startswith("No.") and line.endswith(" Patch"):
                id = line.split("No.")[1].split(" Patch")[0]
                id = int(id)
                patch_ids.append(id)
    # print(patch_ids, rank)
    if rank < len(patch_ids):
        rank = patch_ids[rank]
    else:
        print(llm, proj, idnum, rank)

    path1 = GENERATION_12_PATH.format(llm=GENERATION_LLMS[LLMS.index(llm)], proj=proj, idnum=idnum)
    path2 = GENERATION_20_PATH.format(llm=GENERATION_LLMS[LLMS.index(llm)], proj=proj, idnum=idnum)
    if os.path.exists(path1):
        patch = json.load(open(path1, 'r'))
        if rank >= len(patch):
            print("{llm}_{proj}_{idnum} not exists".format(llm=llm, proj=proj, idnum=idnum))
            sys.exit(-1)
        patch = patch[rank]["diff"]
    elif os.path.exists(path2):
        patch = json.load(open(path2, 'r'))
        if rank >= len(patch):
            print("{llm}_{proj}_{idnum} not exists".format(llm=llm, proj=proj, idnum=idnum))
            sys.exit(-1)
        patch = patch[rank]["diff"]
    else:
        print("{llm}_{proj}_{idnum} not exists".format(llm=llm, proj=proj, idnum=idnum))
    return patch
    

def combine_RQ1():
    RQ1_OUTPUT_DIR  = "/Users/ffengjay/Desktop/GiantRepair_1/results/RQ1/Defects4J_v{version}/{llm}/"
    RQ1_OUTPUT_PATH = "/Users/ffengjay/Desktop/GiantRepair_1/results/RQ1/Defects4J_v{version}/{llm}/{proj}_{idnum}.diff"
    for llm in LLMS:
        fixes_12 = FIXES_12_PATH.format(llm=llm)
        fixes_20 = FIXES_20_PATH.format(llm=llm)
        lines = open(fixes_12, 'r').readlines()
        output_dir = RQ1_OUTPUT_DIR.format(version=12, llm=llm)
        for line in lines:
            line = line.strip()
            if len(line) == 0:
                continue
            proj, idnum, rank = line.split("_")
            diff_out = compare(llm, proj, idnum, int(rank))
            if not os.path.exists(output_dir):
                os.makedirs(output_dir)
            output_file = RQ1_OUTPUT_PATH.format(version=12, llm=llm, proj=proj, idnum=idnum)
            with open(output_file, 'w') as f:
                f.write(diff_out)
        lines = open(fixes_20, 'r').readlines()
        output_dir = RQ1_OUTPUT_DIR.format(version=20, llm=llm)
        for line in lines:
            line = line.strip()
            if len(line) == 0:
                continue
            proj, idnum, rank = line.split("_")
            diff_out = compare(llm, proj, idnum, int(rank))
            if not os.path.exists(output_dir):
                os.makedirs(output_dir)
            output_file = RQ1_OUTPUT_PATH.format(version=20, llm=llm, proj=proj, idnum=idnum)
            with open(output_file, 'w') as f:
                f.write(diff_out)
        # return
        


if __name__ == "__main__":
    combine_RQ1()