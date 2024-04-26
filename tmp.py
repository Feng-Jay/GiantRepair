import os

path = "/data/PLM4APR/codex_out/buggy_methods/"

files = os.listdir(path)

for file in files:
    ori_file = os.path.join(path, file)
    new_file = os.path.join(path, file.lower())
    print(ori_file, new_file)
    os.system("mv {ori_file} {new_file}".format(ori_file=ori_file, new_file=new_file))