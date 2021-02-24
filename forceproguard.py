import argparse
import sys
import fileinput

def main(filename)
    for line in fileinput.input(filename, inplace=True):
        if line.strip().startswith('minifyEnabled') and line.strip().endswith('false') and line.strip():
            line = line.replace('false','true',1);
        sys.stdout.write(line)

if __name__ == '__main__':
    parser = argparse.ArgumentParser("Uncomment some Section X and comment everything else")
    parser.add_argument('file', help='path to build.gradle', type=str)
    args = parser.parse_args()

    main(args.file.strip())
