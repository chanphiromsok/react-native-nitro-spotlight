import { access, cp, mkdir } from 'node:fs/promises';
import { constants } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const source = resolve(root, 'nitrogen/generated/shared/json');
const destination = resolve(root, 'lib/nitrogen/generated/shared/json');

try {
  await access(source, constants.R_OK);
} catch {
  throw new Error(
    `Missing generated Nitro view config JSON at ${source}. Run \`yarn nitrogen\` before packaging.`
  );
}

await mkdir(destination, { recursive: true });
await cp(source, destination, { recursive: true, force: true });

console.log(`Copied Nitro JSON config from ${source} to ${destination}`);
