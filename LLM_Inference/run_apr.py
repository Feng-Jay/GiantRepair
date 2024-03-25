import argparse
import sys
import torch
import os
import json
import time
import random
import numpy as np
import openai
from difflib import unified_diff

from Models.model import GPT2, starCoder, LLama2, CodeLLama
from utils.parse_d4j import clean_parse_d4j, clean_parse_d4j_topN, pick_smallest_example_fix, pick_smallest_example_fix_topN, clean_parse_d4j_expand
from utils.prompt import JAVA_LONG_VARY_PROMPT
from Repair.utils.api_request import request_engine, create_openai_config, create_gpt4_config, create_openai_config_suffix, create_openai_config_single

openai.api_key = ""
openai.api_base = ""


def set_seed(seed: int):
    random.seed(seed)
    np.random.seed(seed)
    torch.manual_seed(seed)
    torch.cuda.manual_seed_all(seed)


def get_unified_diff(source, mutant):
    output = ""
    for line in unified_diff(source.split('\n'), mutant.split('\n'), lineterm=''):
        output += line + "\n"
    return output


def repair_loop(args, model, prompt, file_name, folder, bug, t_chances, skip_val=True):
    start = time.time()
    repair_result = []
    p_diff = {}
    print("Repairing bug {} ... ".format(file_name.split(".")[0]))
    # print(prompt)
    if not model.check_input(prompt, bug['buggy']):
        print("Out of size!")
        return 0, False, False, repair_result

    total_times = 0
    while t_chances > 0:
        total_times += 1
        torch.cuda.empty_cache()
        print("Try :{}".format(total_times))
        well, length, outputs, entropies = model.model_prediction(prompt, bug['buggy'], do_sample=True,
                                                                  num_samples=t_chances)
        t_chances -= args.batch_size
        if well:
            for index, output in enumerate(outputs):
                diff = get_unified_diff(bug['buggy'], output)
                if diff in p_diff:
                    repair_result[p_diff[diff]]['num'] += 1
                    continue
                p_diff[diff] = len(repair_result)
                print(diff)
                repair_result.append({'output': output,
                                      'diff': diff,
                                      'finish_reason': 'stop',
                                      'entropy': entropies[index],
                                      'num': 1})

    end = time.time()

    print("{} Unique Patches Generated in {}s".format(len(repair_result), end - start))

    json_str = json.dumps(repair_result, indent=4)
    with open("{}/{}.json".format(folder, file_name.split(".")[0]), 'w') as f:
        f.write(json_str)

    return len(repair_result), False, False, repair_result


def repair(args, model, bugs, folder, used_prompt, chances, skip_val=True, only_same=True):
    if not os.path.exists(folder):
        os.makedirs(folder)
    with open(folder + "/prompt.txt", "w") as f:
        f.write(used_prompt)
    with open(folder + "/args.txt", "w") as f:
        f.write(str(args))
    all_dataset = clean_parse_d4j_expand("../../")
    result = {}
    t_generated = 0
    t_unique = 0
    start_t = time.time()
    for file_name, bug in bugs.items():
        rerun_list = ["Dosgi_common", "Tika_app", "JacksonDatatypeJsr310", "JacksonModuleAfterburner", "Switchyard_admin", "Switchyard_validate", "Qpidjms_client", "Tiles_api", "Wicket_spring", "Jcodemodel", "Xades4j"]
        if not file_name.split(".java")[0].split("-")[0] in rerun_list:
            continue
        print(file_name)
        # if os.path.exists("{}/{}.json".format(folder, file_name.split(".")[0])):
        #     continue

        if "Collections" in file_name:
            example_bug, example_fix = pick_smallest_example_fix(all_dataset, file_name, only_same=False)
        else:
            example_bug, example_fix = pick_smallest_example_fix(all_dataset, file_name, only_same=only_same)
        prompt = used_prompt.format(example_bug=example_bug, example_fix=example_fix, bug=bug['buggy'])
        n_generated, valid, first_try, result[file_name] = repair_loop(args, model, prompt, file_name, folder, bug,
                                                                       chances, skip_val)
        if n_generated >= 1:
            t_generated += chances
            t_unique += len(result[file_name])

    end_t = time.time()

    with open(folder + "/stats.txt", "w") as f:
        f.write("Total generated: {}\n".format(t_generated))
        f.write("Total unique: {}\n".format(t_unique))
        f.write("Total time: {}\n".format(end_t - start_t))

    with open(folder + "/lm_repair.json", "w") as f:  # write to file
        json.dump(result, f)


def repair_codex_loop(prompt, file_name, folder, bug, t_chances, stop="# Provide a fix for the buggy function",
                skip_val=True) -> (bool, bool, list):
    start = time.time()
    repair_result = []
    p_diff = {}
    print("Repairing bug {} ... ".format(file_name.split(".")[0]))
    print(prompt)
    temperature = 0.8
    top_p = 0.95
    config = create_openai_config(message=prompt, stop=stop, temperature=temperature, top_p=top_p)
    total_times = 0
    while t_chances > 0:
        total_times += 1
        t_chances -= 1
        print("Try: {}".format(total_times))
        ret = request_engine(config)
        if ret is None:
            return False, False, []
        output = ret["choices"][0]['message']['content'].strip()
        diff = get_unified_diff(bug['buggy'], output)
        finish_reason = ret["choices"][0]['finish_reason']
        if finish_reason != "stop":
            continue
        if diff in p_diff:
            repair_result[p_diff[diff]]['num'] += 1
            continue
        p_diff[diff] = len(repair_result)
        print(diff)
        repair_result.append({'output': output,
                              'diff': diff,
                              'finish_reason': finish_reason,
                              'num': 1})

    end = time.time()
    print("{} Unique Patches Generated in {}s".format(len(repair_result), end - start))
    json_str = json.dumps(repair_result, indent=4)
    with open("{}/{}.json".format(folder,file_name.split(".")[0]), 'w') as f:
        f.write(json_str)
    return False, False, repair_result

def repair_codex(args, bugs, folder, used_prompt, chances, stop, skip_val=True, only_same=False):
    """
    Codex repair loop, write each patch to corresponding file
    :param args: arguments
    :param bugs: dict of bugs
    :param folder: folder to save the files
    :param used_prompt: prompt as input to codex
    :param chances: number of chances to try to repair (0 means only try once with temp=1)
    :param vary: whether or not the prompt should be varied (specifically designed for d4j and complex bugs, where the
            we use the an example fix from the same project
    :param stop: stop condition for codex
    :param skip_val: if True, skip validation
    """
    if not os.path.exists(folder):
        os.makedirs(folder)
    with open(folder + "/prompt.txt", "w") as f:
        f.write(used_prompt)
    with open(folder + "/args.txt", "w") as f:
        f.write(str(args))

    result = {}
    t_generated = 0
    t_unique = 0
    start_t = time.time()
    all_dataset = clean_parse_d4j("../../")

    for file_name, bug in bugs.items():
        if os.path.exists("{}/{}.json".format(folder, file_name.split(".")[0])):
            continue
        # print(file_name, bug)
        if "Collections" in file_name:
            example_bug, example_fix = pick_smallest_example_fix_topN(all_dataset, file_name, only_same=False)
        else:
            example_bug, example_fix = pick_smallest_example_fix_topN(all_dataset, file_name, only_same=only_same)
        # example_bug, example_fix = pick_smallest_example_fix(bugs, file_name, only_same=only_same)
        prompt = used_prompt.format(example_bug=example_bug, example_fix=example_fix, bug=bug['buggy'])
        valid, first_try, result[file_name] = repair_codex_loop(prompt, file_name, folder, bug, t_chances=chances,
                                                          stop=stop, skip_val=skip_val)
        if len(result[file_name]) != 0:
            t_generated += chances
            t_unique += len(result[file_name])
        # break
    end_t = time.time()

    with open(folder + "/stats.txt", "w") as f:
        f.write("Total generated: {}\n".format(t_generated))
        f.write("Total unique: {}\n".format(t_unique))
        f.write("Total time: {}\n".format(end_t - start_t))

    with open(folder + "/codex_repair.json", "w") as f:  # write to file
        json.dump(result, f)

def repair_gpt4_loop(prompt, file_name, folder, bug, t_chances, stop="# Provide a fix for the buggy function",
                skip_val=True) -> (bool, bool, list):
    start = time.time()
    repair_result = []
    p_diff = {}
    print("Repairing bug {} ... ".format(file_name.split(".")[0]))
    # print(prompt)
    temperature = 0.8
    top_p = 0.95
    config = create_gpt4_config(message=prompt, stop=stop, temperature=temperature, top_p=top_p)
    total_times = 0
    while t_chances > 0:
        total_times += 1
        t_chances -= 1
        print("Try: {}".format(total_times))
        ret = request_engine(config)
        if ret is None:
            return False, False, []
        output = ret["choices"][0]['message']['content'].strip()
        diff = get_unified_diff(bug['buggy'], output)
        finish_reason = ret["choices"][0]['finish_reason']
        if finish_reason != "stop":
            continue
        if diff in p_diff:
            repair_result[p_diff[diff]]['num'] += 1
            continue
        p_diff[diff] = len(repair_result)
        print(diff)
        repair_result.append({'output': output,
                              'diff': diff,
                              'finish_reason': finish_reason,
                              'num': 1})

    end = time.time()
    print("{} Unique Patches Generated in {}s".format(len(repair_result), end - start))
    json_str = json.dumps(repair_result, indent=4)
    with open("{}/{}.json".format(folder,file_name.split(".")[0]), 'w') as f:
        f.write(json_str)
    return False, False, repair_result

def repair_gpt4(args, bugs, folder, used_prompt, chances, stop, skip_val=True, only_same=False):
    if not os.path.exists(folder):
        os.makedirs(folder)
    with open(folder + "/prompt.txt", "w") as f:
        f.write(used_prompt)
    with open(folder + "/args.txt", "w") as f:
        f.write(str(args))

    all_dataset = clean_parse_d4j("../../")
    result = {}
    t_generated = 0
    t_unique = 0
    start_t = time.time()
    our_fixes_file_path = "../tool_fixes.txt"
    lines = open(our_fixes_file_path).readlines()
    bugids = [bugid.strip() for bugid in lines]
    bugids = random.sample(bugids, 10)
    # print(bugids)
    bugids = ['Closure_36', 'Lang_57', 'Math_85', 'Jsoup_33', 'Closure_113','Closure_19','Math_27','Compress_1','Cli_32','Codec_4']
    for file_name, bug in bugs.items():
        # print(file_name, bug)
        file_tmp = file_name.split(".java")[0].replace('-', '_')
        if file_tmp not in bugids:
            continue
        if "Collections" in file_name:
            example_bug, example_fix = pick_smallest_example_fix_topN(all_dataset, file_name, only_same=False)
        else:
            example_bug, example_fix = pick_smallest_example_fix_topN(all_dataset, file_name, only_same=only_same)
        
        prompt = used_prompt.format(example_bug=example_bug, example_fix=example_fix, bug=bug['buggy'])
        valid, first_try, result[file_name] = repair_codex_loop(prompt, file_name, folder, bug, t_chances=chances,
                                                          stop=stop, skip_val=skip_val)
        if len(result[file_name]) != 0:
            t_generated += chances
            t_unique += len(result[file_name])
        # break
    end_t = time.time()

    with open(folder + "/stats.txt", "w") as f:
        f.write("Total generated: {}\n".format(t_generated))
        f.write("Total unique: {}\n".format(t_unique))
        f.write("Total time: {}\n".format(end_t - start_t))

    with open(folder + "/codex_repair.json", "w") as f:  # write to file
        json.dump(result, f)

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--parameter_path", type=str, help="Downloaded LLM's parameters")
    parser.add_argument("--model_name", type=str, default="EleutherAI/gpt-neo-1.3B")
    parser.add_argument("--batch_size", type=int, default=1)
    parser.add_argument("--dataset", type=str, default="defects4j",
                        help="Dataset to use, current support: defects4j, quixbug-python, quixbugs-java, manybugs")
    parser.add_argument("--chances", type=int, default=1)
    parser.add_argument("--skip_val", action="store_true", default=False)
    parser.add_argument("--folder", type=str, default="../results/test")
    parser.add_argument("--seed", type=int, default=420)
    parser.add_argument("--weight", type=str, default=None)
    args = parser.parse_args()
    if args.dataset == "defects4j":
        print(args.dataset)
        dataset = clean_parse_d4j(folder="../../")
        prompt = JAVA_LONG_VARY_PROMPT
        stop = "// Provide a fix for the buggy function"
        args.language = "java"
    elif args.dataset == "defects4jTop":
        print(args.dataset)
        dataset = clean_parse_d4j_topN(folder="../../")
        prompt = JAVA_LONG_VARY_PROMPT
        stop = "// Provide a fix for the buggy function"
        args.language = "java"
    else:
        print("Unsupported dataset!!!", file=sys.stderr)
        exit(-1)

    set_seed(args.seed)
    model = None
    parameter_path = args.parameter_path
    if args.model_name == "gpt-neo-1.3B":
        model = GPT2(batch_size=args.batch_size, pretrained=args.model_name, stop=stop, weight=args.weight)
    elif args.model_name == "starcoderbase":
        model = starCoder(batch_size= args.batch_size, pretrained=args.model_name, stop=stop, weight=args.weight)
    elif args.model_name == "Llama-2-7b-hf":
        model = LLama2(batch_size= args.batch_size, pretrained=args.model_name, stop=stop, weight=args.weight)
    elif args.model_name == "Llama-2-13b-hf":
        model = LLama2(batch_size= args.batch_size, pretrained=args.model_name, stop=stop, weight=args.weight)
    elif args.model_name == "CodeLlama-7b-hf":
        model = CodeLLama(batch_size= args.batch_size, pretrained=args.model_name, stop=stop, weight=args.weight)
    elif args.model_name == "gpt-3.5":
        repair_codex(args, dataset, args.folder, prompt, chances=args.chances,
                     stop=stop, skip_val=args.skip_val, only_same=args.dataset.startswith("defects4j"))
    elif args.model_name == "gpt-4":
        repair_gpt4(args, dataset, args.folder, prompt, chances=args.chances,
                     stop=stop, skip_val=args.skip_val, only_same=args.dataset.startswith("defects4j"))
    else:
        print("No processed model!!!", file=sys.stderr)
        exit(-1)
    if model != None:
        repair(args, model, dataset, args.folder, prompt, args.chances, args.skip_val,
           only_same=args.dataset.startswith("defects4j"))


if __name__ == '__main__':
    main()
