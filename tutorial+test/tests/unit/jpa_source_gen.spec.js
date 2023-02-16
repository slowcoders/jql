import {describe, expect, test} from '@jest/globals';
import axios from "axios";
import fs from "fs"

describe('JPA Source Generation Test', () => {
  test.each([
    { table: "Character" },
    { table: "Episode" },
    { table: "Starship" },
  ]) ('소스 비교 테스트', async ({table}) => {
    const ref_file = `../sample-app/src/main/java/org/eipgrid/jql/sample/jpa/starwars_jpa/model/${table}.java`;
    let src = fs.readFileSync(ref_file, {encoding:'utf8', flag:'r'});
    src = src.substring(src.indexOf('@'));
    src = src.replace(/\s/g, '');

    const req_url = `http://localhost:7007/api/jql/metadata/starwars/${table}/SpringJPA`
    let res = await axios.get(req_url);
    res = res.data;
    res = res.substring(res.indexOf('@'));
    res = res.replaceAll('"starwars"', '"starwars_jpa"');
    res = res.replace(/\s/g, '');
    expect(src).toBe(res);
  });
});

