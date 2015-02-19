#!/usr/bin/env python3
###############################################################################
##
## Simple utility class to create a forwarded socket connection to an
## application's stetho domain socket.
##
## Usage:
##
##   sock = stetho_open(
##       device='<serial-no>',
##       process='com.facebook.stetho.sample')
##   doHttp(sock)
##
###############################################################################

import socket
import struct

def stetho_open(device=None, process=None):
  adb = _connect_to_device(device)

  socket_name = None
  if process is None:
    socket_name = _find_only_stetho_socket(device)
  else:
    socket_name = _format_process_as_stetho_socket(process)

  try:
    adb.select_service('localabstract:%s' % (socket_name))
  except SelectServiceError as e:
    raise HumanReadableError(
        'Failure to target process %s: %s (is it running?)' % (
            process, e.reason))

  return adb.sock

def _find_only_stetho_socket(device):
  adb = _connect_to_device(device)
  try:
    adb.select_service('shell:cat /proc/net/unix')
    last_stetho_socket_name = None
    process_names = []
    for line in adb.sock.makefile():
      row = line.rstrip().split(' ')
      if len(row) < 8:
        continue
      socket_name = row[7]
      if not socket_name.startswith('@stetho_'):
        continue
      # Filter out entries that are not server sockets
      if int(row[3], 16) != 0x10000 or int(row[5]) != 1:
        continue
      last_stetho_socket_name = socket_name[1:]
      process_names.append(
          _parse_process_from_stetho_socket(socket_name))
    if len(process_names) > 1:
      raise HumanReadableError(
          'Multiple stetho-enabled processes available:%s\n' % (
              '\n\t'.join([''] + process_names)) +
          'Use -p <process> to select one')
    elif last_stetho_socket_name == None:
      raise HumanReadableError('No stetho-enabled processes running')
    else:
      return last_stetho_socket_name
  finally:
    adb.sock.close()

def _connect_to_device(device=None):
  adb = AdbSmartSocketClient()
  adb.connect()

  try:
    if device is None:
      adb.select_service('host:transport-any')
    else:
      adb.select_service('host:transport:%s' % (device))

    return adb
  except SelectServiceError as e:
    raise HumanReadableError(
        'Failure to target device %s: %s' % (device, e.reason))

def _parse_process_from_stetho_socket(socket_name):
  parts = socket_name.split('_')
  if len(parts) < 2 or parts[0] != '@stetho':
    raise Exception('Unexpected Stetho socket formatting: %s' % (socket_name))
  if parts[-2:] == [ 'devtools', 'remote' ]:
    return '.'.join(parts[1:-2])
  else:
    return '.'.join(parts[1:])

def _format_process_as_stetho_socket(process):
  filtered = process.replace('.', '_').replace(':', '_')
  return 'stetho_%s_devtools_remote' % (filtered)

class AdbSmartSocketClient(object):
  """Implements the smartsockets system defined by:
  https://android.googlesource.com/platform/system/core/+/master/adb/protocol.txt
  """

  def __init__(self):
    pass

  def connect(self, port=5037):
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.connect(('127.0.0.1', port))
    self.sock = sock

  def select_service(self, service):
    message = '%04x%s' % (len(service), service)
    self.sock.send(message.encode('ascii'))
    status = self._read_exactly(4)
    if status == b'OKAY':
      # All good...
      pass
    elif status == b'FAIL':
      reason_len = int(self._read_exactly(4), 16)
      reason = self._read_exactly(reason_len).decode('ascii')
      raise SelectServiceError(reason)
    else:
      raise Exception('Unrecognized status=%s' % (status))

  def _read_exactly(self, num_bytes):
    buf = b''
    while len(buf) < num_bytes:
      new_buf = self.sock.recv(num_bytes)
      buf += new_buf
    return buf

class SelectServiceError(Exception):
  def __init__(self, reason):
    self.reason = reason

  def __str__(self):
    return repr(self.reason)

class HumanReadableError(Exception):
  def __init__(self, reason):
    self.reason = reason

  def __str__(self):
    return self.reason
