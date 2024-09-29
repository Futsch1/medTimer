import re
import sys
from glob import glob

results = ''
i = 51
for icon_file in glob(sys.argv[1]):
    with open(icon_file) as f:
        icon = f.read()
    path_matches = re.findall(' d="(.*)"', icon)
    path = ''
    for path_match in path_matches:
        path += path_match
    if not len(path):
        print(icon_file)
    results += '\n<icon id="{}" path="{}"/>'.format(i, path)
    i += 1

print(results)
