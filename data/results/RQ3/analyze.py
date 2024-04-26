"""Analyze ablation study's result
ablation: disable the rank strategy of generated patches
ablation2: disable the selection strategy of modificaions in LLM Patches
ablation3: disable the context-aware strategy used in Skeleton Filling Stage.
ori: all three options are setted
"""
import os
import sys
import logging

LOG_PATH = "./{expr}/logs"
PATCHES_PATH = "./{expr}/patches"

EXPR_SETTINGS = ["ablation", "ablation2", "ablation3", "ori"]

def compilation_rate_helper(file_path):
    if not os.path.exists(file_path):
        logger.error("The file: {} not exists".format(file_path))
        sys.exit(-1)
    lines = open(file_path, 'r').readlines()
    lines.reverse()
    all_failed = -1
    all_patches = -1
    for line in lines:
        line = line.strip()
        if len(line) == 0:
            continue
        if all_failed != -1 and all_patches != -1:
            break
        if line.startswith("Compile failed:"):
            all_failed = int(line.split("Compile failed:")[1].split(" ")[0])
            continue
        if line.startswith("Tested:"):
            all_patches = int(line.split("Tested:")[1].split(" ")[0])
            continue
        
    return all_failed, all_patches

def analyze_compilation_rate(setting):
    log_dirs = LOG_PATH.format(expr=setting)
    repos = os.listdir(log_dirs)
    repo_names = []
    repo_failed = []
    repo_total = []
    for repo in repos:
        repo_names.append(repo)
        one_repo_failed = 0
        one_repo_total = 0
        id_files = os.listdir(os.path.join(LOG_PATH.format(expr=setting), repo))
        for file in id_files:
            bug_failed, bug_total = compilation_rate_helper(os.path.join(LOG_PATH.format(expr=setting), repo, file))
            one_repo_failed += bug_failed
            one_repo_total += bug_total
        repo_failed.append(one_repo_failed), repo_total.append(one_repo_total)
    logger.info(setting)
    logger.info(repo_failed)
    logger.info(repo_total)
    for i, repo in enumerate(repo_names):
        logger.info("{repo_name:<{max_width}} compilation rate is: {rate:.3f}".format(repo_name=repo, 
                                                                            max_width=len(max(repo_names, key=lambda item: len(item))), 
                                                                            rate=1.0 - repo_failed[i]*1.0/repo_total[i]))
    logger.info("{setting} total compilation rate is: {rate:.3f}".format(setting=setting, rate=1.0 - sum(repo_failed) * 1.0 / sum(repo_total)))

def plausible_patches_helper(file_path):
    if not os.path.exists(file_path):
        logger.error("The file: {} not exists".format(file_path))
        sys.exit(-1)
    lines = open(file_path).readlines()
    ret = 0
    for line in lines:
        if line.startswith("Following diff "):
            ret += 1
    return ret


def analyze_plausible_patches(setting):
    patch_dirs = PATCHES_PATH.format(expr=setting)
    repos = os.listdir(patch_dirs) 
    repo_names = []
    repo_plausible = []
    for repo in repos:
        one_repo_plausible = 0
        repo_names.append(repo)
        id_files = os.listdir(os.path.join(PATCHES_PATH.format(expr=setting), repo))
        repo_plausible.append(len(id_files))
        # for file in id_files:
        #     one_repo_plausible += plausible_patches_helper(os.path.join(PATCHES_PATH.format(expr=setting), repo, file))
        # repo_plausible.append(one_repo_plausible)
    
    logger.info(setting)
    logger.info(repo_plausible)
    for i, repo in enumerate(repo_names):
        logger.info("{repo_name:<{max_width}}'s amount of plausible patches is: {amount:3d}".format(repo_name=repo, 
                                                                            max_width=len(max(repo_names, key=lambda item: len(item))), 
                                                                            amount=repo_plausible[i]))
    logger.info("{setting} 's total amount of plausible patches is: {amount:3d}".format(setting=setting, amount=sum(repo_plausible)))

    pass


if __name__ == "__main__":
    logging.basicConfig(level=logging.DEBUG, filename='analyze.log', filemode='w',
                    format='%(name)s - %(levelname)s - %(message)s')
    logger = logging.getLogger("AnalyzeLogger")
    for setting in EXPR_SETTINGS:
        # analyze_compilation_rate(setting=setting)
        analyze_plausible_patches(setting=setting)
        # break