# this file trying to test how many bugs only depend on LLM's output 
import os
import json
import subprocess
import pandas as pd
import javalang
import time
import re
import signal
from utils.DataSet import DataSet

bugs = DataSet('../d4j-info/growing_bugs_single_function.json', '../d4j-info/growing_bugs_filelist.json')

def growing_bugs():
    """
    Get GrowingBugs dataset's buglists as dict:
    {
        project_id :{
            sub_project: "...",
            project_name: "...",
            bug_ids: [...]
        },
        ...
    }"""
    bugs_info_file = "./growingBugsList.xlsx"
    df = pd.read_excel(bugs_info_file)
    df = df.iloc[:, 1:]
    # print(df.columns.tolist())
    bugs_dict = {}
    for index, row in df.iterrows():
        row = list(row)
        # print(row)
        project_id, project_name, sub_project, _, bug_ids = row
        info_dict = {}
        info_dict["sub_project"] = "" if pd.isna(sub_project) else sub_project
        info_dict["project_name"] = project_name
        def parse_ids(ids):
            intervals = ids.split(",")
            ret_ids = []
            for interval in intervals:
                if "-" not in interval:
                    ret_ids.append(int(interval))
                else:
                    begin, end = interval.split("-")
                    for i in range(int(begin), int(end) + 1):
                        ret_ids.append(i)
            return ret_ids
        info_dict["bug_ids"] = parse_ids(bug_ids)
        # print(info_dict)

        bugs_dict[project_id] = info_dict
    # print(bugs_dict)
    return bugs_dict

growing_bugs_dataset = growing_bugs()

def run_d4j_test(source, testmethods, bug_id, workingdir):
    buggy = False
    compile_fail = False
    time_out = False
    entire_buggy = False
    error_string = ""
    
    # check syntax error
    try:
        tokens = javalang.tokenizer.tokenize(source)
        parser = javalang.parser.Parser(tokens)
        parser.parse()
    except:
        return compile_fail, time_out, buggy, entire_buggy, True
    
    for t in testmethods:
        cmd = "defects4j test -w {workingdir} -t {testmethod}".format(workingdir=workingdir, testmethod=t.strip())
        returncode = ""
        error_file = open("stderr.txt", "wb")
        child = subprocess.Popen(cmd, shell=True, stdout=subprocess.PIPE, stderr=error_file, bufsize=-1,
                                 start_new_session=True)
        while_begin = time.time()
        while True:
            Flag = child.poll()
            if Flag == 0:
                # if child.stdout is not None:
                returncode = child.stdout.readlines()  # child.stdout.read()
                print(b"".join(returncode).decode('utf-8'))
                error_file.close()
                break
            elif Flag != 0 and Flag is not None:
                compile_fail = True
                error_file.close()
                with open("stderr.txt", "rb") as f:
                    r = f.readlines()
                for line in r:
                    if re.search(':\serror:\s', line.decode('utf-8')):
                        error_string = line.decode('utf-8')
                        break
                print(error_string)
                break
            elif time.time() - while_begin > 15:
                error_file.close()
                os.killpg(os.getpgid(child.pid), signal.SIGTERM)
                time_out = True
                break
            else:
                time.sleep(0.01)
        log = returncode
        # print("log {}".format(log))
        if len(log) > 0 and log[-1].decode('utf-8') == "Failing tests: 0\n":
            continue
        else:
            buggy = True
            break
        
    if not buggy:
        print('So you pass the basic tests, Check if it passes all the test, include the previously passing tests')
        cmd = "defects4j test -w {workingdir}".format(workingdir= workingdir)
        returncode = ""
        child = subprocess.Popen(cmd, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, bufsize=-1,
                                 start_new_session=True)
        while_begin = time.time()
        while True:
            Flag = child.poll()
            if Flag == 0:
                # if child.stdout is not None:
                returncode = child.stdout.readlines()  # child.stdout.read()
                break
            elif Flag != 0 and Flag is not None:
                buggy = True
                break
            elif time.time() - while_begin > 180:
                os.killpg(os.getpgid(child.pid), signal.SIGTERM)
                buggy = True
                break
            else:
                time.sleep(0.01)
        log = returncode
        if len(log) > 0 and log[-1].decode('utf-8') == "Failing tests: 0\n":
            print('success')
        else:
            entire_buggy = True

    return compile_fail, time_out, buggy, entire_buggy, False


def test_all_patches():
    plausible = 0
    outcome = '../results/starcoderbase_growing_test/'
    rootpath = '../results/starcoderbase_growing/' # where generated patches are stored
    info = bugs.getBugList()
    # print(info)
    #projs = ["Chart", "Closure", "Time", "Math", "Lang"]
    # projs = ["Cli","Codec","Collections","Compress","Csv","Gson","JacksonCore","JacksonDatabind","JacksonXml","Jsoup","JxPath","Mockito"]#change
    for key in info:
        ids = info[key]
        for idnum in ids:
            # if key not in projs:
                # continue
            if os.path.exists("{}/{}_{}.txt".format(outcome, key, idnum)):
                continue
            if not os.path.exists("{}/{}-{}.json".format(rootpath, key, idnum)):
                continue
            print("{}_{}".format(key, idnum))

            subprocess.run("rm -rf ./tmp/defects4j_buggy/{repol}/{repol}_{id}_buggy".format(repol=key.lower(), id=idnum), shell=True)
            if not os.path.exists("./tmp/defects4j_buggy/{repol}/{repol}/{repol}_{id}_buggy".format(id=idnum, repol=key.lower())):
                os.makedirs("./tmp/defects4j_buggy/{repol}/{repol}/{repol}_{id}_buggy".format(id=idnum, repol=key.lower()))
            if len(growing_bugs_dataset[key]["sub_project"]) == 0: 
                subprocess.run('defects4j checkout -p {repo} -v {id}b -w ./tmp/defects4j_buggy/{repol}/{repol}_{id}_buggy'\
                            .format(repo=key, id=idnum, repol=key.lower()), shell=True)
                testmethods = os.popen("defects4j export "
                                   "-w ./tmp/defects4j_buggy/{repol}/{repol}_{id}_buggy "
                                   "-p tests.trigger".format(repol=key.lower(), id=idnum)).readlines()
            else:
                subprocess.run('defects4j checkout -p {repo} -v {id}b -w ./tmp/defects4j_buggy/{repol}/{repol}_{id}_buggy -s {sub}'\
                            .format(repo=key, id=idnum, repol=key.lower(), sub=growing_bugs_dataset[key]["sub_project"]), shell=True)
                testmethods = os.popen("defects4j export "
                                   "-w ./tmp/defects4j_buggy/{repol}/{repol}_{id}_buggy/{sub} "
                                   "-p tests.trigger".format(repol=key.lower(), id=idnum, sub=growing_bugs_dataset[key]["sub_project"])).readlines()
            
            
            
            modifyfile = bugs.getOneBugFile("{}_{}".format(key, idnum))
            
            beginline, endline = bugs.getOneBugLine("{}-{}".format(key, idnum))
            # then check 50 output
            LLMoutputfile = rootpath+"{}-{}.json".format(key, idnum)
            if not os.path.exists(LLMoutputfile):
                with open("./error.txt" ,'a') as f:
                    f.write("No generated patches"+LLMoutputfile+"\n")
                continue
            outputdict = json.load(open(LLMoutputfile, 'r'))
            outputlist = []
            # Original file format
            # for num in outputdict.keys():
            #     if num.isdigit():
            #         outputlist.append(outputdict[num])
            # new file format
            for item in outputdict:
                outputlist.append(item["output"])
            # outputlist = list(set(outputlist))
            tries = len(outputlist)
            originalfile = './tmp/defects4j_buggy/{repo}/{repo}_{id}_buggy/{file}'.format(repo=key.lower(), id=idnum, file=modifyfile)
            
            try:
                with open(originalfile, 'r') as f:
                    sourcelines = f.readlines()
            except:
                with open(originalfile, 'r', encoding="ISO-8859-1") as f:
                    sourcelines = f.readlines()
            
            originalcontent = "".join(sourcelines)
            
            try:
                with open(originalfile, 'r') as f:
                    prior = f.readlines()[0:beginline-1]
            except:
                with open(originalfile, 'r', encoding='ISO-8859-1') as f:
                    prior = f.readlines()[0:beginline-1]
            prior = "".join(prior)

            try:
                with open(originalfile, 'r') as f:
                    after = f.readlines()[endline:]
            except:
                with open(originalfile, 'r', encoding='ISO-8859-1') as f:
                    after = f.readlines()[endline:]
            after = "".join(after)
            
            correctpathes = []
            for i in range(0, tries):
                newfile = prior + "\n" + outputlist[i] + "\n" + after
                with open(originalfile, 'w', encoding='utf-8') as f:
                    f.write(newfile)
                # Begin Testing
                if len(growing_bugs_dataset[key]["sub_project"]) == 0:
                    working_dir = "./tmp/defects4j_buggy/{repo}/{repo}_{id}_buggy/".format(repo=key.lower(), id=idnum)
                else:
                    working_dir = "./tmp/defects4j_buggy/{repo}/{repo}_{id}_buggy/{sub}/".format(repo=key.lower(), id=idnum, sub=growing_bugs_dataset[key]["sub_project"])
                compile_fail, timed_out, buggy, entire_buggy, syntax_error = run_d4j_test(newfile, testmethods, "{}_{}".format(key, idnum), working_dir)
                print("testoutcome: {}, {}, {}, {}, {}".format(
                    compile_fail, timed_out, buggy, entire_buggy, syntax_error))

                if not compile_fail and not timed_out and not buggy and not entire_buggy and not syntax_error:
                    plausible += 1
                    if outputlist[i] in correctpathes:
                        javaf = open(originalfile, 'w', encoding='utf-8')
                        javaf.write(originalcontent)
                        continue
                    if not os.path.exists("{}/{}_{}.txt".format(outcome, key, idnum)):
                        with open("{}/{}_{}.txt".format(outcome, key, idnum), 'w', encoding='utf-8') as f:
                            f.write("No.{} Patch\n".format(i))
                            f.write(outputlist[i]+"\n")
                    else:
                        with open("{}/{}_{}.txt".format(outcome, key, idnum), 'a', encoding='utf-8') as f:
                            f.write("No.{} Patch\n".format(i))
                            f.write(outputlist[i]+"\n")
                    correctpathes.append(outputlist[i])
                javaf = open(originalfile, 'w', encoding='utf-8')
                javaf.write(originalcontent)


if __name__ == "__main__":
    # getDefects4jLines()
    # getDefects4jFiles()
    test_all_patches()
