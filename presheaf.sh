# Generates images; pass file name without extension
[ -f ./instance ] && . ./instance 
[ -f /home/ubuntu/instance ] && . /home/ubuntu/instance
echo "DOING IT..."
#echo "I am `whoami`"
NAME=$1
SRC=$NAME.src
if [ ! -r $CACHE/$SRC ]; then
  echo "Missing source file $CACHE/$SRC"
  exit 1
fi

TEX=$NAME.tex
EPS=$NAME.eps
DVI=$NAME.dvi
IMG=$NAME.png
LOG=$NAME.log
echo "Working on $SRC"
chmod a+r $CACHE/$SRC
chmod a+w $CACHE/$LOG
#ls -l $CACHE/$SRC
grep -q -e "\\\\\\(tikz\\|draw\\|fill\\|filldraw\\|shade\\|path\\|node\\)" $CACHE/$SRC
if [ "$?" -eq "0" ]; then
  $INSTANCE_HOME/dotikz.sh $1
else
  $INSTANCE_HOME/doxy.sh $1
fi
