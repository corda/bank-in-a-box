#!/usr/bin/env bash

################################################################################
# Help                                                                         #
################################################################################
function help() {
  # Display Help
  echo "################################################################################"
  echo "This script is used to deploy Bank in a Box cluster to Kubernetes. This script requires that helm and kubectl
  clients are pre-installed"
  echo
  echo "Syntax: scriptTemplate [-h|action|nodes]"
  echo "options:"
  echo "-h          Print this Help."
  echo "-a          Defines action to be executed, accepts deploy and delete"
  echo "-n          Defines nodes to be deployed / deleted. Default option is all, possible options are bank, notary,
  oracle, web-api, credit-rating and all. Use comma separated list to define multiple nodes."
  echo "################################################################################"
}

ACTION=""
NODES="all"
ALL_NODES="bank|notary|oracle|web-api|credit-rating"
usage() {
  echo "Usage: $0 [ -a ACTION] [ -n NODES]" 1>&2
}

exit_with_usage() {
  usage
  exit 1
}

verify_node_values() {
  for i in ${NODES_ARR[@]}; do
    if [[ ! $i =~ ^(all|$ALL_NODES)$ ]]; then
      echo "Error: Node values can be set only to all|$ALL_NODES"
      exit_with_usage
    fi
  done
}

delete_nodes() {
  for i in ${NODES_ARR[@]}; do
    echo "Deleting node $i"
    if [[ ! $i =~ ^(bank|notary|oracle)$ ]]; then
      helm delete $i-server
    else
      helm delete $i
    fi
  done
}

deploy_nodes() {
  for i in ${NODES_ARR[@]}; do
    echo "Installing node $i"
    if [[ ! $i =~ ^(bank|notary|oracle)$ ]]; then
      helm install $i-server helm/web-server -f helm/web-server/values-$i.yaml
    else
      helm install $i helm/bank-in-a-box -f helm/bank-in-a-box/values-$i.yaml
    fi
  done
}

handle_nodes() {
  if [[ $NODES == *"all"* ]]; then
    NODES_ARR=(${ALL_NODES//|/ })
  fi
  if [ "$ACTION" = "deploy" ]; then
    deploy_nodes
  else
    delete_nodes
  fi
}

while getopts ":a:n:h" options; do
  case "${options}" in
  a)
    ACTION=${OPTARG}
    ;;
  n)
    NODES=${OPTARG}
    ;;
  h)
    help
    exit
    ;;
  :)
    echo "Error: -${OPTARG} requires an argument."
    exit_with_usage
    ;;
  *)
    echo "Error: Unknown option -${OPTARG}."
    exit_with_usage
    ;;
  esac
done

if [[ ! $ACTION =~ ^(deploy|delete)$ ]]; then
  echo "Error: Action must be set to either deploy or delete"
  exit_with_usage
fi

NODES_ARR=(${NODES//,/ })
verify_node_values

handle_nodes

exit 0
