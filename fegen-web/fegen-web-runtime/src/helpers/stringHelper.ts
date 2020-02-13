/**
 * Copyright 2020 Materna Information & Communications SE
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
export class StringHelper {

    constructor() {
        this.generateGuid = this.generateGuid.bind(this);
        this.joinClassNames = this.joinClassNames.bind(this);
        this.joinManyClassNames = this.joinManyClassNames.bind(this);
    }

    public appendParams(url: string, parameters: {[key: string]: string | number | boolean | undefined}): string {
        let result = url;
        let seperator = "?";
        for (const key in parameters) {
            const value = parameters[key];
            if (value === undefined) {
                continue;
            } else {
                result += seperator + key + "=" + encodeURIComponent(value.toString());
                seperator = "&"
            }
        }
        return result;
    }

    public getPercentageString(amount: number, wholeAmount: number, precision?: number): string {
        if (wholeAmount < 0 || amount < 0) { return `Fehler bei Berechnung von % (${amount} von ${wholeAmount})`; }

        if (wholeAmount === 0 && amount === 0) { return ("100%"); }

        const precisionFactor = Math.pow(10, precision || 0);
        return (Math.round((amount / wholeAmount) * 100 * precisionFactor) / precisionFactor) + `%`;
    }

    public generateGuid(): string {

        const first = this.getStringWithFourHexValues() + this.getStringWithFourHexValues();
        const sec = this.getStringWithFourHexValues();
        const third = this.getStringWithFourHexValues();
        const fourth = this.getStringWithFourHexValues();
        const fifth = this.getStringWithFourHexValues() + this.getStringWithFourHexValues() + this.getStringWithFourHexValues();

        return (first + "-" + sec + "-" + third + "-" + fourth + "-" + fifth);
    }


    public getSubstringIfTooLong(value: string, maxLength: number) {
        return value && value.length > maxLength ? value.substr(0, maxLength) : value;
    }

    public getStringWitoutTrailingSlash(url: string) {

        let subResult = url;

        while (subResult && subResult.lastIndexOf("/") === subResult.length - 1) {
            subResult = subResult.substr(0, subResult.length - 1);
        }
        return subResult;
    }

    public joinClassNames(names1: string | undefined | boolean, names2: string | undefined | boolean) {
        const result =
            names1 || names2 ?
                ((names1 ? names1 : "") + (names2 ? `${names1 ? " " : ""}${names2}` : "")) :
                undefined;

        return result;
    }

    public joinManyClassNames(...names: Array<string | undefined | boolean>) {
        let result: string | undefined;
        for (const name of names) {
            result = this.joinClassNames(result, name);
        }
        return result;
    }

    public createCssUrlString(url: string): string | undefined {
        if (!url) { return undefined; }
        return `url(${url})`
    }

    private getStringWithFourHexValues(): string {
        // tslint:disable-next-line:no-bitwise
        return (((1 + Math.random()) * 0x10000) | 0).toString(16).substring(1);
    }
}

export default new StringHelper();
